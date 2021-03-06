package com.onlinebookstore.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.onlinebookstore.common.CommonplaceResult;
import com.onlinebookstore.entity.bookserver.Book;
import com.onlinebookstore.entity.bookserver.BookStorage;
import com.onlinebookstore.entity.orderserver.Order;
import com.onlinebookstore.entity.userserver.Account;
import com.onlinebookstore.entity.userserver.User;
import com.onlinebookstore.exception.IllegalOperateException;
import com.onlinebookstore.mapper.AccountMapper;
import com.onlinebookstore.mapper.UserMapper;
import com.onlinebookstore.service.AccountService;
import com.onlinebookstore.service.BookService;
import com.onlinebookstore.service.OrderService;
import com.onlinebookstore.service.SubtractResidueStrategy;
import com.onlinebookstore.util.AliyunSmsUtil;
import com.onlinebookstore.util.JsonUtil;
import com.onlinebookstore.util.JwtUtil;
import com.onlinebookstore.util.RedisUtils;
import com.onlinebookstore.util.orderutil.OrderOperationStatusEnum;
import com.onlinebookstore.util.rocketmq.RocketMQConstantPool;
import com.onlinebookstore.util.rocketmq.RocketMQMessageSendUtils;
import com.onlinebookstore.util.userutil.UserConstantPool;
import io.seata.core.context.RootContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author rkc
 * @version 3.0
 * @date 2020/9/11 15:43
 */
@Slf4j
@Service
public class AccountServiceImpl implements AccountService {

    @Resource
    private AccountMapper accountMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private BookService bookService;

    @Resource
    private OrderService orderService;

    @Resource
    private JsonUtil jsonUtil;

    @Resource
    private RocketMQMessageSendUtils rocketMQMessageSendUtils;

    @Autowired
    private StringEncryptor encryptor;

    @Autowired
    private SubtractStrategyFactory subtractStrategyFactory;

    @Autowired
    private RedisUtils redisUtils;
    private static final String forgotPasswordBlacklist = "forgotPassword:blacklist";
    private static final String forgotPasswordSentCode = "forgotPassword:sentCode";
    private final ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(15, 30,
            5, TimeUnit.SECONDS, new ArrayBlockingQueue<>(20));

    /**
     * 忘记密码，通过手机短信找回
     * @param account 账号
     * @return CommonplaceResult
     */
    @Override
    public CommonplaceResult forgotPassword(Account account, String phone) {
        Account accountUser = accountMapper.getAccountContainUserByUsername(account.getUsername());
        if (ObjectUtils.isEmpty(accountUser)) return CommonplaceResult.buildErrorNoData("没有该用户！");
        if (!accountUser.getUser().getPhone().equals(phone)) return CommonplaceResult.buildErrorNoData("手机号码错误！");
        //查询redis，是否是黑名单，不是黑名单则继续发送验证码
        if (ObjectUtils.isEmpty(redisUtils.get(forgotPasswordBlacklist + account.getUsername()))) {
            //发送短信验证码
            String code = AliyunSmsUtil.getCode();
            JSONObject jsonObject = AliyunSmsUtil.sendSms(accountUser.getUser().getPhone(), code);
            if (jsonObject.getInteger("code") == AliyunSmsUtil.SEND_SUCCESS) {
                poolExecutor.execute(() -> {
                    //存入redis，并设置3分钟后值自动过期
                    redisUtils.set(forgotPasswordSentCode + account.getUsername(), code, 180);
                    //加入黑名单，2分钟内不能再次发送
                    redisUtils.set(forgotPasswordBlacklist + account.getUsername(), true, 120);
                    log.info("验证码发送成功：{}", code);
                });
                return CommonplaceResult.buildSuccess(jsonObject, "发送成功！");
            }
            return CommonplaceResult.buildError(jsonObject, "发送失败！");
        }
        return CommonplaceResult.buildErrorNoData("您发送次数过多，请稍后再试！");
    }

    /**
     * 重置密码
     * @param account 账号
     * @param code 验证码
     * @return CommonplaceResult
     */
    @Override
    public CommonplaceResult resetPassword(Account account, String code) {
        String correctCode = String.valueOf(redisUtils.get(forgotPasswordSentCode + account.getUsername()));
        if (StringUtils.isEmpty(correctCode)) return CommonplaceResult.buildErrorNoData("验证码已过期或不存在！");
        if (code.equals(correctCode)) {
            account.setPassword(encryptor.encrypt(account.getPassword()));
            poolExecutor.execute(() -> {
                //删除redis中用户的验证码
                redisUtils.del(forgotPasswordSentCode + account.getUsername());
            });
            //验证码正确，调用dao层进行修改密码
            return accountMapper.modifyPassword(account.getUsername(), account.getPassword()) > 0 ? CommonplaceResult.buildSuccessNoData("修改成功")
                    : CommonplaceResult.buildErrorNoData("修改失败");
        }
        return CommonplaceResult.buildErrorNoData("验证码输入错误！");
    }

    /**
     * 充值余额
     * @param username 账号
     * @param count 数量
     * @return CommonplaceResult
     */
    @Override
    public CommonplaceResult topUpResidue(String username, int count) {
        //TODO 结合微信支付，应该先扣除微信余额再进行添加，并且需要进行事务控制
        if (count <= 0) return CommonplaceResult.buildErrorNoData("非法操作！");
        return accountMapper.modifyBalance(username, count) > 0 ? CommonplaceResult.buildSuccessNoData("充值成功！") :
                CommonplaceResult.buildErrorNoData("充值失败！");
    }

    /**
     * 创建订单
     * @param order 订单
     */
    @Override
    public CommonplaceResult createOrder(Order order) {
        order.setSerialNumber(UUID.randomUUID().toString());
        order.setCreateTime(new Date());
        order.setOrderPaymentStatus(0);
        //创建订单
        CommonplaceResult result = orderService.insertOrder(order);
        if (result.getCode() == 0) return CommonplaceResult.buildErrorNoData(result.getMessage());
        //发送异步延时消息，测试为5分钟取消，上线改为30分钟
        rocketMQMessageSendUtils.sendDelayMessageAsync(RocketMQConstantPool.Topic.R_ORDER_IMPORT, OrderOperationStatusEnum.CANCEL.status,
                order, new SendCallback() {
                    @Override
                    public void onSuccess(SendResult sendResult) {
                        log.info("取消订单的延时消息发送成功！" + sendResult.getSendStatus());
                    }
                    @Override
                    public void onException(Throwable e) {
                        log.error("取消订单的延时消息发送失败！" + e.getMessage());
                    }
                }, 9);
        return CommonplaceResult.buildSuccess(order, "订单创建成功");
    }

    /**
     * 添加账户，首先查询该账户是否已经添加，如果已经添加则返回，
     * 否则继续进行，中途有任何的异常都会结束该事务操作
     * @param account 账户实体类
     * @return 影响行数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommonplaceResult addAccount(Account account, User user) {
        //如果查询是空，说明该账号没有被注册，可以进行注册操作，否则返回
        if (ObjectUtils.isEmpty(accountMapper.selectOneByUsername(account.getUsername()))) {
            //加密
            account.setPassword(encryptor.encrypt(account.getPassword()));
            //账号注册
            accountMapper.addAccount(account);
            //用户信息注册
            userMapper.addUser(user);
            return CommonplaceResult.buildSuccessNoData("注册成功！");
        }
        return CommonplaceResult.buildErrorNoData("该账号已被注册！");
    }

    /**
     * JWT单点登录
     * @param username 账号
     * @param password 密码
     * @return 账户实体类
     */
    @Override
    public CommonplaceResult selectAccountByUsernameAndPassword(String username, String password) {
        //得到密文
        String ciphertext = accountMapper.getPasswordByUsername(username);
        if (StringUtils.isEmpty(ciphertext)) return CommonplaceResult.buildErrorNoData("登录失败");
        //解密匹配
        if (encryptor.decrypt(ciphertext).equals(password)) {
            password = ciphertext;
            Account account = accountMapper.selectAccountByUsernameAndPassword(username, password);
            if (ObjectUtils.isEmpty(account)) {
                return CommonplaceResult.buildErrorNoData("账号或者密码错误！");
            }
            //登录成功，生成jwt令牌，返回到客户端
            Map<String, Object> info = new HashMap<>(5);
            //基于工具类生成jwt token
            String token = JwtUtil.createJWT(UUID.randomUUID().toString(), account.getUsername(), null);
            //将token携带回去，每次发起请求都需要在请求头中携带token，便于网关进行拦截验证
            info.put(UserConstantPool.TOKEN, token);
            //登录成功，将封装好的账户+用户对象返回，避免前端再次请求获取用户信息数据
            info.put(UserConstantPool.ACCOUNT, account);
            return CommonplaceResult.buildSuccess(info, "登录成功！");
        }
        return CommonplaceResult.buildErrorNoData("账号或者密码错误！");
    }

    /**
     * 查询所有的账户，通常由管理员调用
     * @return 账户集合列表
     */
    @Override
    public CommonplaceResult selectAllAccount() {
        return CommonplaceResult.buildSuccessNoMessage(accountMapper.selectAllAccount());
    }

    /**
     * 修改密码
     * @param newPassword 新密码
     * @return CommonplaceResult
     */
    @Override
    public CommonplaceResult modifyPasswordByUsername(String username, String oldPassword, String newPassword) {
        return accountMapper.modifyPasswordByUsername(username, oldPassword, newPassword) > 0 ? CommonplaceResult.buildSuccessNoData("修改成功")
                : CommonplaceResult.buildErrorNoData("修改失败，账号或密码错误！");
    }

    /**
     * 根据账号查询全部信息，包括用户信息的关联查询
     * @param username 账号
     * @return 账号信息+用户信息
     */
    @Override
    public CommonplaceResult getAccountContainUserByUsername(String username) {
        Account accountWithUser = accountMapper.getAccountContainUserByUsername(username);
        if (accountWithUser != null) accountWithUser.setPassword("");
        return accountWithUser == null ? CommonplaceResult.buildErrorNoData("获取失败，不存在该用户！") :
                CommonplaceResult.buildSuccess(accountWithUser, "获取成功！");
    }

    /**
     * 需要进行逻辑处理，如果查询的账号的积分为0且modifyNumber为负数，
     * 又或者积分数不够扣除，均为扣除失败
     * @param username 账号
     * @param modifyNumber 修改的积分
     * @return 是否修改成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommonplaceResult modifyScore(String username, Integer modifyNumber) {
        //查询积分信息
        Account account = accountMapper.selectOneByUsername(username);
        if (account == null) {
            return CommonplaceResult.buildErrorNoData("账号异常！");
        }
        int score = account.getScore();
        //积分为0，不能进行扣除，非法操作
        if (score == 0 && modifyNumber < 0) {
            throw new IllegalOperateException();
        }
        //积分不足扣除
        if (score < Math.abs(modifyNumber) && modifyNumber < 0) {
            return CommonplaceResult.buildErrorNoData("积分不足，扣除失败！");
        }
        //正常扣除或者添加
        int row = accountMapper.modifyScoreByUsername(username, modifyNumber);
        return row > 0 ? CommonplaceResult.buildSuccessNoData("修改成功！") :
                CommonplaceResult.buildErrorNoData("服务器异常");
    }

    /**
     * 购买图书，需要使用全局事务处理
     * TODO 后续整合seata和调用订单微服务
     * 流程：检查用户余额 -> 调用订单微服务创建订单 -> 扣除用户余额 -> 调用图书微服务扣除库存 -> 修改订单状态为完成订单
     * @param serialNumber 订单号
     */
    @Override
    @SuppressWarnings("all")
    @Transactional(rollbackFor = Exception.class)
//    @GlobalTransactional(name = "dealWithOrder", rollbackFor = Exception.class)
    public CommonplaceResult purchaseBook(String serialNumber) {
        //查询订单
        Order order = null;
        try {
            CommonplaceResult result = orderService.selectOrderBySerialNumber(serialNumber);
            if (result.getCode() == 1) order = jsonUtil.mapToBean((Map) result.getData(), Order.class);
            else return CommonplaceResult.buildErrorNoData(result.getMessage());
        } catch (Exception e) {
            log.error("对象转换错误：" + e.getMessage());
            return CommonplaceResult.buildErrorNoData("没有该数据！");
        }
        if (ObjectUtils.isEmpty(order)) {
            return CommonplaceResult.buildErrorNoData("没有该数据！");
        }

        //如果订单已经过期，直接返回
        if (order.getOrderPaymentStatus() == -1) {
            return CommonplaceResult.buildErrorNoData("订单过期！购买失败");
        }
//        log.info("订单信息：" + String.valueOf(order));
        //获取订单中的基本信息
        int bookId = order.getBookId();
        int count = order.getProductCount();
        String username = order.getUsernameId();
        //为0或者为null都认为是不使用积分的消费
        boolean useScore = order.getUseScore() == 1;

        //调用图书微服务，得到图书信息+库存信息，通过断点调试可以得知，此时得到的对象是LinkedHashMap
        CommonplaceResult res = bookService.selectBookAndStorageByBookId(bookId);
        Object data = res.getData();
        if (data instanceof Boolean) {
            //服务调用失败
            return CommonplaceResult.buildErrorNoData(res.getMessage());
        }
        //转化为Book实体类
        try {
            Book book = jsonUtil.mapToBean((Map) data, Book.class);
            if (!ObjectUtils.isEmpty(book)) {
                BookStorage bookStorage = book.getBookStorage();
                if (count > bookStorage.getResidueCount()) {
                    return CommonplaceResult.buildErrorNoData("库存不足，购买失败！");
                }
                //得到需要支付的金额
                int money = book.getPrice() * count;
                int score = money / 10;
                String orderContent = "购买了" + count + "本《" + book.getBookName() + "》";
                order.setPaymentTime(new Date());
                log.info("user-server，xid：" + RootContext.getXID());

                //2、扣除余额
                CommonplaceResult commonplaceResult = this.modifyBalance(username, -money, useScore);
                if ((boolean) commonplaceResult.getData()) {
                    //扣除成功，则进行下一步，扣除库存
                    Map<String, Integer> pojo = new HashMap<>();
                    pojo.put("id", bookStorage.getId());
                    pojo.put("count", count);
                    //3、扣除库存
                    CommonplaceResult storageCalledResult = bookService.subtractStorageById(pojo);
                    if (ObjectUtils.isEmpty(storageCalledResult.getData())) {
                        log.error("返回值为空！！！");
                        throw new RuntimeException("server occurred a error during calling subtracting storage");
                    }
                    if ((boolean) storageCalledResult.getData()) {
                        order.setOrderPaymentStatus(1);
                        order.setDeliveryTime(new Date());
                        order.setEndTime(new Date());
                        order.setObtainScore(score);
                        order.setWholePrice(money);

                        //4、修改订单状态和其它数据信息
                        CommonplaceResult result = orderService.updateOrder(order);
                        log.info("result.getCode() {}", result.getCode());
                        if (result.getCode() != 1) throw new RuntimeException("订单状态修改失败！");
                        this.modifyScore(username, score);
                        return CommonplaceResult.buildSuccessNoData("操作成功！");
                    } else {
                        return CommonplaceResult.buildErrorNoData(storageCalledResult.getMessage());
                    }
                } else {
                    return CommonplaceResult.buildErrorNoData("余额不足！");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return CommonplaceResult.buildSuccessNoData("服务器异常！");
    }

    /**
     * 修改余额
     * @param username 账号
     * @param count 修改的数量，如果为负数则为扣除，否则为充值
     * @param useScore 是否使用抵扣，只有在count为负数时才有效，count为正数时为null
     * @return 操作是否成功
     */
    @Override
    public CommonplaceResult modifyBalance(String username, int count, boolean useScore) {
        if (StringUtils.isEmpty(username)) {
            return CommonplaceResult.buildError(false, "非法账号！");
        }
        Account account = accountMapper.selectOneByUsername(username);
        log.info("修改余额操作：" + account);
        if (ObjectUtils.isEmpty(account)) {
            return CommonplaceResult.buildError(false, "非法操作！账号不存在");
        }
        if (count > 0) {
            //充值操作
            return addBalance(username, count);
        } else if (count < 0) {
            //消费操作，首先进行余额判断
            if (account.getBalance() < Math.abs(count)) {
                return CommonplaceResult.buildError(false, "余额不足！");
            }
            return subtractBalance(account, count, useScore);
        }
        return CommonplaceResult.buildError(false, "修改数量不能为0！");
    }

    /**
     * 扣除余额逻辑，判断是否使用优惠券，如果使用积分，如果使用，则进行相应的抵扣，10积分=1余额，且最多只能抵扣10元
     * @param account 操作账号
     * @param count 操作数量
     * @param useScore 是否使用积分
     */
    private CommonplaceResult subtractBalance(Account account, Integer count, Boolean useScore) {
        //根据useScore，由工厂类决定使用哪种扣费策略
        SubtractResidueStrategy subtractResidueStrategy = subtractStrategyFactory.getSubtractResidueStrategy(useScore);
        return subtractResidueStrategy.subtractBalance(account, count);
    }

    /**
     * 充值金额
     * @param username 操作账号
     * @param count 充值数量
     */
    private CommonplaceResult addBalance(String username, Integer count) {
        return accountMapper.modifyBalance(username, count) > 0 ? CommonplaceResult.buildSuccessNoData("充值成功") :
                CommonplaceResult.buildErrorNoData("充值失败！");
    }
}
