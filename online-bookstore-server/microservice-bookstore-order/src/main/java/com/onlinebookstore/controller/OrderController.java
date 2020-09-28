package com.onlinebookstore.controller;

import com.onlinebookstore.common.CommonplaceResult;
import com.onlinebookstore.entity.Order;
import com.onlinebookstore.exception.StatusCodeException;
import com.onlinebookstore.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


import javax.annotation.Resource;
import java.util.Map;

/**
 * @author rkc
 * @date 2020/9/21 8:29
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/order")
public class OrderController {

    @Resource
    private OrderService orderService;

    /**
     * 查询所有订单
     */
    @GetMapping("pri/selectAll")
    public CommonplaceResult selectAll() {
        return orderService.selectAll();
    }

    /**
     * 根据账号查询所有订单
     * @param username 账号
     */
    @GetMapping("pri/selectAllByUsername/{username}")
    public CommonplaceResult selectAllByUsername(@PathVariable(value = "username") String username) {
        return orderService.selectAllByUsername(username);
    }

    /**
     * 根据图书id查询该书本的所有订单
     * @param bookId 图书id
     */
    @GetMapping("pri/selectAllByBookId/{bookId}")
    public CommonplaceResult selectAllByBookId(@PathVariable("bookId") Integer bookId) {
        return orderService.selectAllByBookId(bookId);
    }

    /**
     * 新建订单
     * @param order 订单对象
     * @return 影响行数
     */
    @PostMapping("pri/insertOrder")
    public CommonplaceResult insertOrder(@RequestBody Order order) {
        return orderService.insertOrder(order);
    }

    /**
     * 更新订单状态
     * 数据格式：
     * {
     *     'code': 'xx',
     *     'serial_number': 'xxx'
     * }
     * @param pojo 包含状态码和订单号
     */
    @PostMapping("pri/updateOrderStatus")
    public CommonplaceResult updateOrderStatus(@RequestBody Map<String, String> pojo) {
        int code = Integer.parseInt(pojo.get("code"));
        String serialNumber = pojo.get("serial_number");
        if (code < -1 || code > 1) {
            throw new StatusCodeException(code, "状态码非法！");
        }
        return orderService.updateOrderStatus(code, serialNumber);
    }

    /**
     * 更新订单
     * @param order 订单对象
     */
    @PostMapping("pri/updateOrder")
    public CommonplaceResult updateOrder(@RequestBody Order order) {
        return orderService.updateOrder(order);
    }
}