package com.xq.tmall.controller.fore;

import com.alibaba.fastjson.JSONObject;
import com.xq.tmall.controller.BaseController;
import com.xq.tmall.entity.*;
import com.xq.tmall.service.*;
import com.xq.tmall.util.OrderUtil;
import com.xq.tmall.util.PageUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Controller
public class ForeOrderController extends BaseController {
    @Resource(name = "productService")
    private ProductService productService;
    @Resource(name = "userService")
    private UserService userService;
    @Resource(name = "productOrderItemService")
    private ProductOrderItemService productOrderItemService;
    @Resource(name = "addressService")
    private AddressService addressService;
    @Resource(name = "categoryService")
    private CategoryService categoryService;
    @Resource(name = "productImageService")
    private ProductImageService productImageService;
    @Resource(name = "productOrderService")
    private ProductOrderService productOrderService;
    @Resource(name = "reviewService")
    private ReviewService reviewService;
    @Resource(name = "lastIDService")
    private LastIDService lastIDService;

    //转到前台天猫-订单列表页
    @RequestMapping(value = "order/{index}/{count}", method = RequestMethod.GET)
    public String goToPage(HttpSession session, Map<String, Object> map,
                           @RequestParam(required = false) Byte status,
                           @PathVariable("index") Integer index/* 页数 */,
                           @PathVariable("count") Integer count/* 行数*/) {
        logger.info("检查用户是否登录");
        Object userId = checkUser(session);
        User user;
        if (userId != null) {
            logger.info("获取用户信息");
            user = userService.get(Integer.parseInt(userId.toString()));
            map.put("user", user);
        } else {
            return "redirect:/login";
        }
        Byte[] status_array = null;
        if (status != null) {
            status_array = new Byte[]{status};
        }

        PageUtil pageUtil = new PageUtil(index, count);
        logger.info("根据用户ID:{}获取订单列表", userId);
        List<ProductOrder> productOrderList = productOrderService.getList(new ProductOrder().setProductOrder_user(new User().setUser_id(Integer.valueOf(userId.toString()))), status_array, new OrderUtil("productOrder_status"), pageUtil);

        //订单总数量
        Integer orderCount = 0;
        if (productOrderList.size() > 0) {
            orderCount = productOrderService.getTotal(new ProductOrder().setProductOrder_user(new User().setUser_id(Integer.valueOf(userId.toString()))), status_array);
            logger.info("获取订单项信息及对应的产品信息");
            for (ProductOrder order : productOrderList) {
                List<ProductOrderItem> productOrderItemList = productOrderItemService.getListByOrderId(order.getProductOrder_id(), null);
                if (productOrderItemList != null) {
                    for (ProductOrderItem productOrderItem : productOrderItemList) {
                        Integer product_id = productOrderItem.getProductOrderItem_product().getProduct_id();
                        Product product = productService.get(product_id);
                        product.setSingleProductImageList(productImageService.getList(product_id, (byte) 0, new PageUtil(0, 1)));
                        productOrderItem.setProductOrderItem_product(product);
                        if (status != null && status == 3) {
                            productOrderItem.setReview(reviewService.getTotalByOrderItemId(productOrderItem.getProductOrderItem_id()) > 0);
                        }
                    }
                }
                order.setProductOrderItemList(productOrderItemList);
            }
        }
        pageUtil.setTotal(orderCount);

        logger.info("获取产品分类列表信息");
        List<Category> categoryList = categoryService.getList(null, new PageUtil(0, 5));

        map.put("pageUtil", pageUtil);
        map.put("productOrderList", productOrderList);
        map.put("categoryList", categoryList);
        map.put("status", status);

        logger.info("转到前台天猫-订单列表页");
        return "fore/orderListPage";
    }

    //转到前台天猫-订单确认页
    @RequestMapping(value = "order/buy/{product_id}", method = RequestMethod.GET)
    public String goToOrderConfirmPage(@PathVariable("product_id") Integer product_id,
                                       @RequestParam(required = false) Short product_number,
                                       Map<String, Object> map,
                                       HttpSession session,
                                       HttpServletRequest request) throws UnsupportedEncodingException {
        logger.info("检查用户是否登录");
        Object userId = checkUser(session);
        User user;
        if (userId != null) {
            logger.info("获取用户信息");
            user = userService.get(Integer.parseInt(userId.toString()));
            map.put("user", user);
        } else {
            return "redirect:/login";
        }
        if (product_number == null) {
            return "redirect:/product/" + product_id;
        }
        logger.info("通过产品ID获取产品信息：{}", product_id);
        Product product = productService.get(product_id);
        if (product == null) {
            return "redirect:/";
        }
        logger.info("获取产品的详细信息");
        product.setProduct_category(categoryService.get(product.getProduct_category().getCategory_id()));
        product.setSingleProductImageList(productImageService.getList(product_id, (byte) 0, new PageUtil(0, 1)));

        logger.info("封装订单项对象");
        ProductOrderItem productOrderItem = new ProductOrderItem();
        productOrderItem.setProductOrderItem_product(product);
        productOrderItem.setProductOrderItem_number(product_number);
        productOrderItem.setProductOrderItem_price(product.getProduct_sale_price() * product_number);
        productOrderItem.setProductOrderItem_user(new User().setUser_id(Integer.valueOf(userId.toString())));

        String addressId = "110000";
        String cityAddressId = "110100";
        String districtAddressId = "110101";
        String detailsAddress = null;
        String order_post = null;
        String order_receiver = null;
        String order_phone = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                String cookieName = cookie.getName();
                String cookieValue = cookie.getValue();
                switch (cookieName) {
                    case "addressId":
                        addressId = cookieValue;
                        break;
                    case "cityAddressId":
                        cityAddressId = cookieValue;
                        break;
                    case "districtAddressId":
                        districtAddressId = cookieValue;
                        break;
                    case "order_post":
                        order_post = URLDecoder.decode(cookieValue, "UTF-8");
                        break;
                    case "order_receiver":
                        order_receiver = URLDecoder.decode(cookieValue, "UTF-8");
                        break;
                    case "order_phone":
                        order_phone = URLDecoder.decode(cookieValue, "UTF-8");
                        break;
                    case "detailsAddress":
                        detailsAddress = URLDecoder.decode(cookieValue, "UTF-8");
                        break;
                }
            }
        }
        logger.info("获取省份信息");
        List<Address> addressList = addressService.getRoot();
        logger.info("获取addressId为{}的市级地址信息", addressId);
        List<Address> cityAddress = addressService.getList(null, addressId);
        logger.info("获取cityAddressId为{}的区级地址信息", cityAddressId);
        List<Address> districtAddress = addressService.getList(null, cityAddressId);

        List<ProductOrderItem> productOrderItemList = new ArrayList<>();
        productOrderItemList.add(productOrderItem);

        map.put("orderItemList", productOrderItemList);
        map.put("addressList", addressList);
        map.put("cityList", cityAddress);
        map.put("districtList", districtAddress);
        map.put("orderTotalPrice", productOrderItem.getProductOrderItem_price());

        map.put("addressId", addressId);
        map.put("cityAddressId", cityAddressId);
        map.put("districtAddressId", districtAddressId);
        map.put("order_post", order_post);
        map.put("order_receiver", order_receiver);
        map.put("order_phone", order_phone);
        map.put("detailsAddress", detailsAddress);

        logger.info("转到前台天猫-订单确认页");
        return "fore/productBuyPage";
    }

    //转到前台天猫-订单支付页
    @RequestMapping(value = "order/pay/{order_code}", method = RequestMethod.GET)
    public String goToOrderPayPage(Map<String, Object> map, HttpSession session,
                                   @PathVariable("order_code") String order_code) {
        logger.info("检查用户是否登录");
        Object userId = checkUser(session);
        User user;
        if (userId != null) {
            logger.info("获取用户信息");
            user = userService.get(Integer.parseInt(userId.toString()));
            map.put("user", user);
        } else {
            return "redirect:/login";
        }
        logger.info("------验证订单信息------");
        logger.info("查询订单是否存在");
        ProductOrder order = productOrderService.getByCode(order_code);
        if (order == null) {
            logger.warn("订单不存在，返回订单列表页");
            return "redirect:/order/0/1";
        }
        logger.info("验证用户与订单是否一致");
        if (order.getProductOrder_user().getUser_id() != Integer.parseInt(userId.toString())) {
            logger.warn("用户与订单信息不一致，返回订单列表页");
            return "redirect:/order/0/1";
        }
        order.setProductOrderItemList(productOrderItemService.getListByOrderId(order.getProductOrder_id(), null));

        double orderTotalPrice = 0.00;
        if (order.getProductOrderItemList().size() == 1) {
            logger.info("获取单订单项的产品信息");
            ProductOrderItem productOrderItem = order.getProductOrderItemList().get(0);
            Product product = productService.get(productOrderItem.getProductOrderItem_product().getProduct_id());
            product.setProduct_category(categoryService.get(product.getProduct_category().getCategory_id()));
            productOrderItem.setProductOrderItem_product(product);
            orderTotalPrice = productOrderItem.getProductOrderItem_price();
        } else {
            for (ProductOrderItem productOrderItem : order.getProductOrderItemList()) {
                orderTotalPrice += productOrderItem.getProductOrderItem_price();
            }
        }
        logger.info("订单总金额为：{}元", orderTotalPrice);

        map.put("productOrder", order);
        map.put("orderTotalPrice", orderTotalPrice);
        return "fore/productPayPage";
    }

    //创建新订单-单订单项-ajax
    @ResponseBody
    @RequestMapping(value = "order", method = RequestMethod.POST, produces = "application/json;charset=utf-8")
    public String createOrderByOne(HttpSession session, Map<String, Object> map, HttpServletResponse response,
                                   @RequestParam String addressId,
                                   @RequestParam String cityAddressId,
                                   @RequestParam String districtAddressId,
                                   @RequestParam String productOrder_detail_address,
                                   @RequestParam String productOrder_post,
                                   @RequestParam String productOrder_receiver,
                                   @RequestParam String productOrder_mobile,
                                   @RequestParam String userMessage,
                                   @RequestParam Integer orderItem_product_id,
                                   @RequestParam Short orderItem_number) {
        JSONObject object = new JSONObject();
        logger.info("检查用户是否登录");
        Object userId = checkUser(session);
        User user;
        if (userId != null) {
            logger.info("获取用户信息");
            user = userService.get(Integer.parseInt(userId.toString()));
            map.put("user", user);
        } else {
            object.put("success", false);
            object.put("url", "/login");
            return object.toJSONString();
        }
        Product product = productService.get(orderItem_product_id);
        if (product == null) {
            object.put("success", false);
            object.put("url", "/");
            return object.toJSONString();
        }
        try {
            logger.info("将收货地址等相关信息存入Cookie中");
            Cookie cookie1 = new Cookie("addressId", addressId);
            Cookie cookie2 = new Cookie("cityAddressId", cityAddressId);
            Cookie cookie3 = new Cookie("districtAddressId", districtAddressId);
            Cookie cookie4 = new Cookie("order_post", URLEncoder.encode(productOrder_post, "UTF-8"));
            Cookie cookie5 = new Cookie("order_receiver", URLEncoder.encode(productOrder_receiver, "UTF-8"));
            Cookie cookie6 = new Cookie("order_phone", URLEncoder.encode(productOrder_mobile, "UTF-8"));
            Cookie cookie7 = new Cookie("detailsAddress", URLEncoder.encode(productOrder_detail_address, "UTF-8"));
            int maxAge = 60 * 60 * 24 * 365;  //设置过期时间为一年
            cookie1.setMaxAge(maxAge);
            cookie2.setMaxAge(maxAge);
            cookie3.setMaxAge(maxAge);
            cookie4.setMaxAge(maxAge);
            cookie5.setMaxAge(maxAge);
            cookie6.setMaxAge(maxAge);
            cookie7.setMaxAge(maxAge);
            response.addCookie(cookie1);
            response.addCookie(cookie2);
            response.addCookie(cookie3);
            response.addCookie(cookie4);
            response.addCookie(cookie5);
            response.addCookie(cookie6);
            response.addCookie(cookie7);
        } catch (Exception e) {
            e.printStackTrace();
        }

        StringBuffer productOrder_code = new StringBuffer()
                .append(new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()))
                .append(0)
                .append(userId);
        logger.info("生成的订单号为：{}", productOrder_code);
        logger.info("整合订单对象");
        ProductOrder productOrder = new ProductOrder()
                .setProductOrder_status((byte) 0)
                .setProductOrder_address(new Address().setAddress_areaId(districtAddressId))
                .setProductOrder_post(productOrder_post)
                .setProductOrder_user(new User().setUser_id(Integer.valueOf(userId.toString())))
                .setProductOrder_mobile(productOrder_mobile)
                .setProductOrder_receiver(productOrder_receiver)
                .setProductOrder_detail_address(productOrder_detail_address)
                .setProductOrder_pay_date(new Date())
                .setProductOrder_code(productOrder_code.toString());
        Boolean yn = productOrderService.add(productOrder);
        if (!yn) {
            throw new RuntimeException();
        }
        Integer order_id = lastIDService.selectLastID();
        logger.info("整合订单项对象");
        ProductOrderItem productOrderItem = new ProductOrderItem()
                .setProductOrderItem_user(new User().setUser_id(Integer.valueOf(userId.toString())))
                .setProductOrderItem_product(productService.get(orderItem_product_id))
                .setProductOrderItem_number(orderItem_number)
                .setProductOrderItem_price(product.getProduct_sale_price() * orderItem_number)
                .setProductOrderItem_userMessage(userMessage)
                .setProductOrderItem_order(new ProductOrder().setProductOrder_id(order_id));
        yn = productOrderItemService.add(productOrderItem);
        if (!yn) {
            throw new RuntimeException();
        }

        object.put("success", true);
        object.put("url", "/order/pay/" + productOrder.getProductOrder_code());
        return object.toJSONString();
    }
}
