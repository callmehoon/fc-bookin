package bookstore_ai_project.controller;

import bookstore_ai_project.dto.response.LoginResponse;
import bookstore_ai_project.entity.*;
import bookstore_ai_project.repository.*;
import bookstore_ai_project.service.OrderService;
import bookstore_ai_project.service.CartService;
import bookstore_ai_project.dto.response.ProductSimpleResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.List;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;

@Controller
public class OrderController {
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private CartService cartService;

    @Value("${kakaopay.admin-key}")
    private String adminKey;
    @Value("${kakaopay.cid}")
    private String cid;

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderDetailRepository orderDetailRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StockRepository stockRepository;

    // 상품상세에서 단일 상품 구매 (GET)
    @GetMapping("/order")
    public String orderSingle(@RequestParam("isbn") String isbn,
                              @RequestParam(value = "quantity", required = false, defaultValue = "1") int quantity,
                              Model model, HttpSession session) {
        // 🔒 로그인 검증: 비로그인 시 로그인 페이지로 리다이렉트
        Object userObj = session.getAttribute("user");
        if (userObj == null || !(userObj instanceof LoginResponse.UserInfo)) {
            return "redirect:/login?redirectUrl=/order?isbn=" + isbn + "&quantity=" + quantity;
        }
        
        // 단일 상품 정보 조회
        List<ProductSimpleResponse> orderList = orderService.getOrderProductList(List.of(isbn), List.of(quantity), session);
        model.addAttribute("orderList", orderList);
        // 로그인 유저 정보도 모델에 추가 (예시)
        model.addAttribute("loginUser", session.getAttribute("user"));
        return "order/order";
    }

    // 장바구니에서 선택/전체 상품 구매 (POST)
    @PostMapping("/order")
    public String orderMultiple(@RequestParam("isbns") List<String> isbns,
                                @RequestParam("quantities") List<Integer> quantities,
                                Model model, HttpSession session) {
        // 🔒 로그인 검증: 비로그인 시 로그인 페이지로 리다이렉트
        Object userObj = session.getAttribute("user");
        if (userObj == null || !(userObj instanceof LoginResponse.UserInfo)) {
            return "redirect:/login?redirectUrl=/cart"; // 장바구니로 리다이렉트 (POST 데이터 보존 어려움)
        }
        
        // 여러 상품 정보 조회
        List<ProductSimpleResponse> orderList = orderService.getOrderProductList(isbns, quantities, session);
        model.addAttribute("orderList", orderList);
        model.addAttribute("loginUser", session.getAttribute("user"));
        return "order/order";
    }

    // 단일 상품 바로구매(주문 생성) API
    @PostMapping("/order/buy")
    @ResponseBody
    public Map<String, Object> buyNow(@RequestBody Map<String, Object> body, HttpSession session) {
        Object userObj = session.getAttribute("user");
        Map<String, Object> result = new HashMap<>();
        if (userObj == null) {
            result.put("error", "NOT_LOGGED_IN");
            return result;
        }
        String idForAdmin = userObj instanceof User ? ((User) userObj).getIdForAdmin() :
                userObj instanceof LoginResponse.UserInfo ? ((LoginResponse.UserInfo) userObj).getIdForAdmin() : null;
        if (idForAdmin == null) {
            result.put("error", "USER_ERROR");
            return result;
        }
        String isbn = (String) body.get("isbn");
        int quantity = body.get("quantity") != null ? (int) body.get("quantity") : 1;
        // 주문 생성 로직 (임시: orderId는 System.currentTimeMillis() 사용)
        // 실제로는 OrderService에 주문 생성 메서드 구현 필요
        long orderId = System.currentTimeMillis();
        // TODO: orderService.createOrder(idForAdmin, isbn, quantity) 등으로 실제 주문 생성
        result.put("orderId", orderId);
        return result;
    }

    // 주문 생성 (페이지 진입 시)
    @PostMapping("/api/order/create")
    @ResponseBody
    public Map<String, Object> createOrder(@RequestBody Map<String, Object> req, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Object userObj = session.getAttribute("user");
            if (userObj == null || !(userObj instanceof LoginResponse.UserInfo userInfo)) {
                result.put("success", false);
                result.put("message", "로그인이 필요합니다.");
                return result;
            }
            String idForAdmin = userInfo.getIdForAdmin();
            List<Map<String, Object>> products = (List<Map<String, Object>>) req.get("products");
            String orderId = (String) req.get("orderId");
            String orderDateStr = (String) req.get("orderDate");
            System.out.println("[DEBUG] /api/order/create: orderId=" + orderId + ", idForAdmin=" + idForAdmin + ", orderDate=" + orderDateStr + ", products.size=" + (products != null ? products.size() : 0));
            if (products == null || products.isEmpty() || orderId == null) {
                result.put("success", false);
                result.put("message", "상품 정보 또는 주문번호가 필요합니다.");
                return result;
            }
            // 중복 체크
            var orderOpt = orderRepository.findById(new OrderId(orderId, idForAdmin));
            if (orderOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "이미 존재하는 주문번호입니다. 새로고침 후 다시 시도해 주세요.");
                return result;
            }
            for (Map<String, Object> p : products) {
                String isbn = (String) p.get("isbn");
                if (isbn == null || !productRepository.existsByIsbn(isbn)) {
                    result.put("success", false);
                    result.put("message", "존재하지 않는 상품입니다: " + isbn);
                    return result;
                }
            }
            int totalProductCategory = products.size();
            int totalProductQuantity = products.stream().mapToInt(p -> (int) p.getOrDefault("quantity", 1)).sum();
            // 🛡️ 보안 강화: 서버에서 실제 가격으로 재계산
            int totalProductPrice = 0;
            for (Map<String, Object> p : products) {
                String isbn = (String) p.get("isbn");
                int quantity = (int) p.getOrDefault("quantity", 1);
                Optional<Product> product = productRepository.findById(isbn);
                if (product.isPresent()) {
                    totalProductPrice += product.get().getPrice() * quantity;
                }
            }
            
            // 배송비 계산 (총상품금액 20,000원 이상 시 무료, 미만 시 3,000원)
            int shippingFee = totalProductPrice >= 20000 ? 0 : 3000;
            
            // 최종 결제금액 계산 (총상품금액 + 배송비)
            int totalPaidPrice = totalProductPrice + shippingFee;
            
            Order order = new Order();
            order.setOrderId(orderId);
            order.setIdForAdmin(idForAdmin);
            order.setOrderStatus(Order.OrderStatus.ORDER_REQUESTED);
            order.setOrderDate(orderDateStr != null ? LocalDateTime.parse(orderDateStr) : LocalDateTime.now());
            order.setTotalProductCategory(totalProductCategory);
            order.setTotalProductQuantity(totalProductQuantity);
            order.setTotalPaidPrice(totalPaidPrice);
            orderRepository.save(order);
            System.out.println("[DEBUG] 주문 insert 완료: orderId=" + orderId + ", idForAdmin=" + idForAdmin);
            for (Map<String, Object> p : products) {
                String isbn = (String) p.get("isbn");
                int quantity = (int) p.getOrDefault("quantity", 1);
                
                // 🛡️ 보안 강화: 서버에서 실제 가격 조회 및 검증
                Optional<Product> product = productRepository.findById(isbn);
                if (product.isEmpty()) {
                    result.put("success", false);
                    result.put("message", "존재하지 않는 상품입니다.");
                    return result;
                }
                
                Product actualProduct = product.get();
                int actualUnitPrice = actualProduct.getPrice();
                int actualTotalPrice = actualUnitPrice * quantity;
                
                // 클라이언트 가격과 실제 가격 비교 (보안 검증)
                int clientUnitPrice = (int) p.getOrDefault("unitPrice", 0);
                int clientTotalPrice = (int) p.getOrDefault("totalPrice", 0);
                
                if (clientUnitPrice != actualUnitPrice || clientTotalPrice != actualTotalPrice) {
                    result.put("success", false);
                    result.put("message", "상품 가격이 변경되었습니다. 페이지를 새로고침 후 다시 시도해주세요.");
                    return result;
                }
                
                OrderDetail detail = new OrderDetail();
                detail.setOrderId(orderId);
                detail.setIdForAdmin(idForAdmin);
                detail.setIsbn(isbn);
                detail.setOrderItemQuantity(quantity);
                detail.setEachProductPrice(actualUnitPrice);    // 서버에서 검증된 가격 사용
                detail.setTotalProductPrice(actualTotalPrice);  // 서버에서 계산된 가격 사용
                orderDetailRepository.save(detail);
            }
            result.put("success", true);
            result.put("orderId", orderId);
            result.put("idForAdmin", idForAdmin);
            result.put("message", "주문이 생성되었습니다.");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "주문 처리 중 문제가 발생했습니다. 잠시 후 다시 시도해주세요.");
        }
        return result;
    }

    // 주문 상태변경 (이탈/실패/성공 등)
    @PostMapping("/api/order/update-status")
    @ResponseBody
    public Map<String, Object> updateOrderStatus(HttpServletRequest request) {
        System.out.println("=== 주문상태변경 API 호출됨 ===");
        Map<String, Object> result = new HashMap<>();
        try {
            String body = request.getReader().lines().collect(Collectors.joining());
            System.out.println("[DEBUG] /api/order/update-status body: " + body);
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> req = mapper.readValue(body, Map.class);
            String orderId = (String) req.get("orderId");
            String idForAdmin = (String) req.get("idForAdmin");
            String status = (String) req.get("status");
            System.out.println("[DEBUG] orderId=" + orderId + ", idForAdmin=" + idForAdmin + ", status=" + status);
            if (orderId == null || idForAdmin == null || status == null) {
                result.put("success", false);
                result.put("message", "필수값 누락");
                return result;
            }
            var orderOpt = orderRepository.findById(new OrderId(orderId, idForAdmin));
            System.out.println("[DEBUG] orderOpt.isEmpty() = " + orderOpt.isEmpty());
            if (orderOpt.isEmpty()) {
                result.put("success", false);
                result.put("message", "주문을 찾을 수 없습니다.");
                return result;
            }
            Order order = orderOpt.get();
            Order.OrderStatus newStatus = Order.OrderStatus.valueOf(status);
            order.setOrderStatus(newStatus);
            order.setUpdateDate(LocalDateTime.now());
            orderRepository.save(order);
            System.out.println("[DEBUG] order status updated: " + order.getOrderStatus());
            
            // ORDER_FAILED 상태로 변경 시 관련 Payment들도 FAILED로 변경
            if (newStatus == Order.OrderStatus.ORDER_FAILED) {
                List<Payment> payments = paymentRepository.findByIdOrderIdAndIdIdForAdmin(orderId, idForAdmin);
                System.out.println("[DEBUG] ORDER_FAILED 처리 - 관련 Payment 개수: " + payments.size());
                
                for (Payment payment : payments) {
                    if (payment.getPaymentStatus() == Payment.PaymentStatus.PAYMENT_ATTEMPT) {
                        payment.setPaymentStatus(Payment.PaymentStatus.PAYMENT_FAILED);
                        payment.setUpdateDate(LocalDateTime.now());
                        paymentRepository.save(payment);
                        System.out.println("[DEBUG] Payment 상태 FAILED로 변경: " + payment.getId().getPaymentId());
                    }
                }
            }
            
            // PREPARING_PRODUCT 상태로 변경 시 Stock OUTBOUND 기록 추가
            if (newStatus == Order.OrderStatus.PREPARING_PRODUCT) {
                try {
                    // 주문 상세 정보 조회
                    List<OrderDetail> orderDetails = orderDetailRepository.findByOrderIdAndIdForAdmin(orderId, idForAdmin);
                    
                    for (OrderDetail detail : orderDetails) {
                        // 현재 재고량 조회
                        List<Integer> stockList = stockRepository.findCurrentStockListByIsbn(detail.getIsbn(), org.springframework.data.domain.PageRequest.of(0, 1));
                        int beforeQuantity = stockList.isEmpty() ? 0 : stockList.get(0);
                        int outQuantity = detail.getOrderItemQuantity();
                        int afterQuantity = beforeQuantity - outQuantity;
                        
                        // Stock OUTBOUND 기록 생성
                        Stock stockRecord = new Stock();
                        stockRecord.setIsbn(detail.getIsbn());
                        stockRecord.setInOutType(Stock.InOutType.OUTBOUND);
                        stockRecord.setInOutQuantity(outQuantity);
                        stockRecord.setBeforeQuantity(beforeQuantity);
                        stockRecord.setAfterQuantity(afterQuantity);
                        stockRecord.setUpdateDate(LocalDateTime.now());
                        
                        stockRepository.save(stockRecord);
                        System.out.println("[DEBUG] Stock OUTBOUND 기록 추가: " + detail.getIsbn() + 
                                         ", 수량: " + outQuantity + ", 이전: " + beforeQuantity + ", 이후: " + afterQuantity);
                    }
                } catch (Exception e) {
                    System.err.println("[ERROR] Stock OUTBOUND 기록 추가 중 오류: " + e.getMessage());
                    // Stock 기록 실패는 주문 상태 변경을 막지 않음
                }
            }
            
            result.put("success", true);
            result.put("message", "주문 상태가 변경되었습니다.");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "주문 상태변경 중 오류: " + e.getMessage());
        }
        return result;
    }

    // 결제 시도 (PAYMENT_ATTEMPT) - 최대한 강화된 디버깅 버전
    @PostMapping("/api/payment/attempt")
    @ResponseBody
    public Map<String, Object> paymentAttempt(HttpServletRequest request) {
        System.out.println("==================================================");
        System.out.println("🔵 /api/payment/attempt API 호출됨");
        System.out.println("🔵 요청 시각: " + LocalDateTime.now());
        System.out.println("==================================================");

        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 요청 본문 읽기
            String body = request.getReader().lines().collect(Collectors.joining());
            System.out.println("🔵 요청 Body: " + body);

            if (body == null || body.trim().isEmpty()) {
                System.out.println("❌ 요청 body가 비어있음");
                result.put("success", false);
                result.put("message", "요청 body가 비어있습니다.");
                return result;
            }

            // 2. JSON 파싱
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> req = mapper.readValue(body, Map.class);
            System.out.println("🔵 파싱된 JSON: " + req);

            // 3. 필드 추출
            String orderId = (String) req.get("orderId");
            String idForAdmin = (String) req.get("idForAdmin");
            String paymentId = (String) req.get("paymentId");
            String paymentMethod = (String) req.get("paymentMethod");
            String paymentDateStr = (String) req.get("paymentDate"); // 이 줄 추가!

            System.out.println("🔵 추출된 필드들:");
            System.out.println("  - orderId: '" + orderId + "' (null: " + (orderId == null) + ")");
            System.out.println("  - idForAdmin: '" + idForAdmin + "' (null: " + (idForAdmin == null) + ")");
            System.out.println("  - paymentId: '" + paymentId + "' (null: " + (paymentId == null) + ")");
            System.out.println("  - paymentMethod: '" + paymentMethod + "' (null: " + (paymentMethod == null) + ")");
            System.out.println("  - paymentDate: '" + paymentDateStr + "' (null: " + (paymentDateStr == null) + ")");

            // 4. 필수값 검증
            if (orderId == null || idForAdmin == null || paymentId == null || paymentMethod == null) {
                System.out.println("❌ 필수값 누락 감지");
                result.put("success", false);
                result.put("message", "필수값 누락");
                return result;
            }

            // 5. PaymentId 중복 체크
            System.out.println("🔵 PaymentId 중복 체크 시작: " + paymentId);
            Optional<Payment> existingPayment = paymentRepository.findByIdPaymentIdAndIdOrderIdAndIdIdForAdmin(paymentId, orderId, idForAdmin);
            System.out.println("🔵 기존 Payment 존재 여부: " + existingPayment.isPresent());

            if (existingPayment.isPresent()) {
                System.out.println("❌ 이미 존재하는 paymentId: " + paymentId);
                result.put("success", false);
                result.put("message", "이미 존재하는 결제번호입니다.");
                return result;
            }

            // 6. Order 존재 여부 확인
            System.out.println("🔵 Order 존재 여부 확인: orderId=" + orderId + ", idForAdmin=" + idForAdmin);
            Optional<Order> orderOpt = orderRepository.findById(new OrderId(orderId, idForAdmin));
            if (orderOpt.isEmpty()) {
                System.out.println("❌ 해당 주문을 찾을 수 없음");
                result.put("success", false);
                result.put("message", "해당 주문을 찾을 수 없습니다.");
                return result;
            }

            Order order = orderOpt.get();
            System.out.println("🔵 주문 찾음: " + order.getOrderId() + " (상태: " + order.getOrderStatus() + ")");

            // 7. 기존 PAYMENT_ATTEMPT 상태 결제건들을 FAILED로 변경
            System.out.println("🔵 기존 결제 시도 건들 실패 처리 시작");
            List<Payment> existingAttempts = paymentRepository.findByIdOrderIdAndIdIdForAdmin(orderId, idForAdmin);
            for (Payment attemptPayment : existingAttempts) {
                if (attemptPayment.getPaymentStatus() == Payment.PaymentStatus.PAYMENT_ATTEMPT) {
                    attemptPayment.setPaymentStatus(Payment.PaymentStatus.PAYMENT_FAILED);
                    attemptPayment.setUpdateDate(LocalDateTime.now());
                    paymentRepository.save(attemptPayment);
                    System.out.println("🔵 기존 결제 시도 실패 처리: " + attemptPayment.getId().getPaymentId());
                }
            }

            // 8. 새로운 Payment 엔티티 생성
            System.out.println("🔵 새로운 Payment 엔티티 생성 시작");
            Payment payment = new Payment();
            PaymentId paymentIdObj = new PaymentId(paymentId, orderId, idForAdmin);
            payment.setId(paymentIdObj);
            payment.setPaymentMethodId(paymentMethod);
            payment.setPaymentStatus(Payment.PaymentStatus.PAYMENT_ATTEMPT);

            // 수정: 무조건 전달받은 날짜 사용, 없으면 오류 처리
            LocalDateTime paymentDate;
            if (paymentDateStr != null && !paymentDateStr.isEmpty()) {
                try {
                    paymentDate = LocalDateTime.parse(paymentDateStr);
                    System.out.println("✅ 프론트에서 전달받은 날짜 사용: " + paymentDate);
                } catch (Exception e) {
                    System.out.println("❌ 날짜 파싱 실패: " + e.getMessage());
                    result.put("success", false);
                    result.put("message", "날짜 형식이 올바르지 않습니다: " + paymentDateStr);
                    return result;
                }
            } else {
                System.out.println("❌ paymentDate가 전달되지 않음");
                result.put("success", false);
                result.put("message", "결제 날짜가 전달되지 않았습니다.");
                return result;
            }

            payment.setPaymentDate(paymentDate);

            // 9. DB 저장
            System.out.println("🔵 Payment DB 저장 시작...");
            Payment savedPayment = paymentRepository.save(payment);
            System.out.println("✅ Payment DB 저장 완료!");
            System.out.println("✅ 저장된 Payment ID: " + savedPayment.getId().getPaymentId());
            System.out.println("✅ 저장된 Payment 상태: " + savedPayment.getPaymentStatus());

            // 10. 저장 검증
            System.out.println("🔵 저장 검증 시작...");
            List<Payment> verifyPayments = paymentRepository.findByIdOrderId(orderId);
            System.out.println("🔵 해당 주문의 총 Payment 개수: " + verifyPayments.size());

            for (Payment p : verifyPayments) {
                System.out.println("  - Payment: " + p.getId().getPaymentId() + " (상태: " + p.getPaymentStatus() + ")");
            }

            // 11. 성공 응답
            result.put("success", true);
            result.put("message", "결제 시도 기록이 저장되었습니다.");
            result.put("paymentId", savedPayment.getId().getPaymentId());
            result.put("paymentStatus", savedPayment.getPaymentStatus().toString());

            System.out.println("✅ API 처리 성공!");

        } catch (Exception e) {
            System.out.println("❌ Payment API 처리 중 오류 발생:");
            System.out.println("❌ 오류 타입: " + e.getClass().getSimpleName());
            System.out.println("❌ 오류 메시지: " + e.getMessage());
            e.printStackTrace();

            result.put("success", false);
            result.put("message", "결제 시도 저장 중 오류: " + e.getMessage());
        }

        System.out.println("🔵 최종 응답: " + result);
        System.out.println("==================================================");
        return result;
    }

    // 결제 성공 (PAYMENT_COMPLETED, 주문도 ORDER_COMPLETED) - 개선된 버전
    @PostMapping("/api/payment/complete")
    @ResponseBody
    @Transactional // 결제 완료와 장바구니 삭제를 하나의 트랜잭션으로 처리
    public Map<String, Object> paymentComplete(@RequestBody Map<String, Object> req, HttpSession session) {
        System.out.println("=== /api/payment/complete API 호출됨 ===");
        Map<String, Object> result = new HashMap<>();

        try {
            Object userObj = session.getAttribute("user");
            if (userObj == null || !(userObj instanceof LoginResponse.UserInfo userInfo)) {
                result.put("success", false);
                result.put("message", "로그인이 필요합니다.");
                return result;
            }

            String idForAdmin = userInfo.getIdForAdmin();
            String orderId = (String) req.get("orderId");

            System.out.println("[DEBUG] 결제 완료 처리: orderId=" + orderId + ", idForAdmin=" + idForAdmin);

            if (orderId == null) {
                result.put("success", false);
                result.put("message", "주문번호가 필요합니다.");
                return result;
            }

            // 해당 주문의 모든 payment 상태를 COMPLETED로 변경
            List<Payment> payments = paymentRepository.findByIdOrderIdAndIdIdForAdmin(orderId, idForAdmin);
            System.out.println("[DEBUG] 찾은 payment 개수: " + payments.size());

            for (Payment payment : payments) {
                // PAYMENT_ATTEMPT 상태인 것만 COMPLETED로 변경
                if (payment.getPaymentStatus() == Payment.PaymentStatus.PAYMENT_ATTEMPT) {
                    payment.setPaymentStatus(Payment.PaymentStatus.PAYMENT_COMPLETED);
                    payment.setUpdateDate(LocalDateTime.now());
                    paymentRepository.save(payment);
                    System.out.println("[DEBUG] Payment 완료 처리: " + payment.getId().getPaymentId());
                }
            }

            // order 상태도 변경
            Optional<Order> orderOpt = orderRepository.findById(new OrderId(orderId, idForAdmin));
            if (orderOpt.isPresent()) {
                Order order = orderOpt.get();
                order.setOrderStatus(Order.OrderStatus.ORDER_COMPLETED);
                order.setUpdateDate(LocalDateTime.now());
                orderRepository.save(order);
                System.out.println("[DEBUG] Order 완료 처리: " + order.getOrderId());
            }

            // 🛒 결제 완료 시 장바구니에서 주문한 상품들 자동 삭제
            List<OrderDetail> orderDetails = orderDetailRepository.findByOrderIdAndIdForAdmin(orderId, idForAdmin);
            System.out.println("[DEBUG] 주문완료로 장바구니에서 삭제할 상품 수: " + orderDetails.size());
            
            for (OrderDetail detail : orderDetails) {
                try {
                    cartService.deleteCartItem(idForAdmin, detail.getIsbn());
                    System.out.println("[DEBUG] 장바구니에서 삭제 완료: ISBN=" + detail.getIsbn());
                } catch (Exception e) {
                    System.out.println("[WARN] 장바구니 삭제 실패 (상품이 이미 없을 수 있음): ISBN=" + detail.getIsbn() + ", 오류=" + e.getMessage());
                    // 장바구니 삭제 실패는 주문 완료를 막지 않음 (이미 결제가 완료된 상황이므로)
                }
            }

            result.put("success", true);
            result.put("message", "결제 성공 및 주문완료 처리되었습니다.");

        } catch (Exception e) {
            System.out.println("[ERROR] 결제 성공 처리 중 오류:");
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "결제 성공 처리 중 오류: " + e.getMessage());
        }

        System.out.println("[DEBUG] 결제 완료 처리 응답: " + result);
        return result;
    }

    // 결제 실패/이탈 (PAYMENT_FAILED, 주문도 ORDER_FAILED) - 개선된 버전
    @PostMapping("/api/payment/fail")
    @ResponseBody
    public Map<String, Object> paymentFail(@RequestBody Map<String, Object> req, HttpSession session) {
        System.out.println("=== /api/payment/fail API 호출됨 ===");
        Map<String, Object> result = new HashMap<>();

        try {
            Object userObj = session.getAttribute("user");
            if (userObj == null || !(userObj instanceof LoginResponse.UserInfo userInfo)) {
                result.put("success", false);
                result.put("message", "로그인이 필요합니다.");
                return result;
            }

            String idForAdmin = userInfo.getIdForAdmin();
            String orderId = (String) req.get("orderId");

            System.out.println("[DEBUG] 결제 실패 처리: orderId=" + orderId + ", idForAdmin=" + idForAdmin);

            if (orderId == null) {
                result.put("success", false);
                result.put("message", "주문번호가 필요합니다.");
                return result;
            }

            // 해당 주문의 모든 payment 상태를 FAILED로 변경
            List<Payment> payments = paymentRepository.findByIdOrderIdAndIdIdForAdmin(orderId, idForAdmin);
            System.out.println("[DEBUG] 찾은 payment 개수: " + payments.size());

            for (Payment payment : payments) {
                // PAYMENT_ATTEMPT 상태인 것만 FAILED로 변경
                if (payment.getPaymentStatus() == Payment.PaymentStatus.PAYMENT_ATTEMPT) {
                    payment.setPaymentStatus(Payment.PaymentStatus.PAYMENT_FAILED);
                    payment.setUpdateDate(LocalDateTime.now());
                    paymentRepository.save(payment);
                    System.out.println("[DEBUG] Payment 실패 처리: " + payment.getId().getPaymentId());
                }
            }

            // 주문 상태는 변경하지 않음 (기존 결제 시도만 정리하는 목적)
            // 실제 결제 실패시에만 주문을 실패로 변경해야 함
            System.out.println("[DEBUG] 기존 결제건만 실패 처리 완료 - 주문 상태는 유지");

            result.put("success", true);
            result.put("message", "기존 결제건 정리가 완료되었습니다.");

        } catch (Exception e) {
            System.out.println("[ERROR] 결제 실패 처리 중 오류:");
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "결제 실패 처리 중 오류: " + e.getMessage());
        }

        System.out.println("[DEBUG] 결제 실패 처리 응답: " + result);
        return result;
    }

    // 주문 취소 API (주문 페이지에서 메인 페이지로 이동할 때 호출)
    @PostMapping("/api/order/cancel")
    @ResponseBody
    public Map<String, Object> cancelOrder(@RequestBody Map<String, Object> req, HttpSession session) {
        System.out.println("=== /api/order/cancel API 호출됨 ===");
        Map<String, Object> result = new HashMap<>();

        try {
            Object userObj = session.getAttribute("user");
            if (userObj == null || !(userObj instanceof LoginResponse.UserInfo userInfo)) {
                result.put("success", false);
                result.put("message", "로그인이 필요합니다.");
                return result;
            }

            String idForAdmin = userInfo.getIdForAdmin();
            String orderId = (String) req.get("orderId");

            System.out.println("[DEBUG] 주문 취소 처리: orderId=" + orderId + ", idForAdmin=" + idForAdmin);

            if (orderId == null) {
                result.put("success", false);
                result.put("message", "주문번호가 필요합니다.");
                return result;
            }

            // 1. 주문 상태를 FAILED로 변경
            Optional<Order> orderOpt = orderRepository.findById(new OrderId(orderId, idForAdmin));
            if (orderOpt.isEmpty()) {
                result.put("success", false);
                result.put("message", "해당 주문을 찾을 수 없습니다.");
                return result;
            }

            Order order = orderOpt.get();
            order.setOrderStatus(Order.OrderStatus.ORDER_FAILED);
            order.setUpdateDate(LocalDateTime.now());
            orderRepository.save(order);
            System.out.println("[DEBUG] 주문 상태 FAILED로 변경: " + order.getOrderId());

            // 2. 관련 Payment들도 모두 FAILED로 변경
            List<Payment> payments = paymentRepository.findByIdOrderIdAndIdIdForAdmin(orderId, idForAdmin);
            System.out.println("[DEBUG] 관련 Payment 개수: " + payments.size());

            for (Payment payment : payments) {
                if (payment.getPaymentStatus() == Payment.PaymentStatus.PAYMENT_ATTEMPT) {
                    payment.setPaymentStatus(Payment.PaymentStatus.PAYMENT_FAILED);
                    payment.setUpdateDate(LocalDateTime.now());
                    paymentRepository.save(payment);
                    System.out.println("[DEBUG] Payment 상태 FAILED로 변경: " + payment.getId().getPaymentId());
                }
            }

            result.put("success", true);
            result.put("message", "주문이 취소되었습니다.");

        } catch (Exception e) {
            System.out.println("[ERROR] 주문 취소 처리 중 오류:");
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "주문 취소 처리 중 오류: " + e.getMessage());
        }

        System.out.println("[DEBUG] 주문 취소 처리 응답: " + result);
        return result;
    }

    // OrderController.java에 추가할 메서드

    // 결제완료(주문완료) summary 페이지 매핑 - 수정됨
    @GetMapping("/order/summary")
    public String orderSummary(@RequestParam(required = false) String orderId,
                               @RequestParam(required = false) String idForAdmin,
                               Model model, HttpSession session) {

        System.out.println("=== /order/summary 진입 ===");
        System.out.println("파라미터 orderId: " + orderId);
        System.out.println("파라미터 idForAdmin: " + idForAdmin);

        // 1. 파라미터로 받지 못한 경우 세션에서 가져오기 시도
        if (orderId == null || idForAdmin == null) {
            Object userObj = session.getAttribute("user");
            if (userObj instanceof LoginResponse.UserInfo userInfo) {
                idForAdmin = userInfo.getIdForAdmin();
                System.out.println("세션에서 가져온 idForAdmin: " + idForAdmin);
            }
        }

        if (orderId == null || idForAdmin == null) {
            System.out.println("주문정보를 찾을 수 없어 메인으로 리다이렉트");
            return "redirect:/main";
        }

        try {
            // 2. 주문 정보 조회
            Optional<Order> orderOpt = orderRepository.findById(new OrderId(orderId, idForAdmin));
            if (orderOpt.isEmpty()) {
                System.out.println("주문을 찾을 수 없음: " + orderId);
                return "redirect:/main";
            }

            Order order = orderOpt.get();
            System.out.println("주문 정보 조회 성공: " + order.getOrderId());

            // 3. 주문 상세 정보 조회
            List<OrderDetail> orderDetails = orderDetailRepository.findByOrderIdAndIdForAdmin(orderId, idForAdmin);
            System.out.println("주문 상세 개수: " + orderDetails.size());

            // 4. 결제 정보 조회
            List<Payment> payments = paymentRepository.findByIdOrderIdAndIdIdForAdmin(orderId, idForAdmin);
            Payment latestPayment = payments.isEmpty() ? null : payments.get(payments.size() - 1);

            // 5. 사용자 정보 조회
            var userOpt = userRepository.findByIdForAdmin(idForAdmin);
            if (userOpt.isEmpty()) {
                System.out.println("사용자를 찾을 수 없음: " + idForAdmin);
                return "redirect:/main";
            }
            var user = userOpt.get();

            // 6. 주문 상품 정보 구성
            List<Map<String, Object>> productList = new ArrayList<>();
            for (OrderDetail detail : orderDetails) {
                var productOpt = productRepository.findById(detail.getIsbn());
                if (productOpt.isPresent()) {
                    var product = productOpt.get();
                    Map<String, Object> productInfo = new HashMap<>();
                    productInfo.put("isbn", detail.getIsbn());
                    productInfo.put("productName", product.getProductName());
                    productInfo.put("author", product.getAuthor());
                    productInfo.put("img", product.getImg());
                    productInfo.put("quantity", detail.getOrderItemQuantity());
                    productInfo.put("unitPrice", detail.getEachProductPrice());
                    productInfo.put("totalPrice", detail.getTotalProductPrice());
                    productList.add(productInfo);
                }
            }

            // 7. 배송비 계산 - 상품 가격들의 합을 먼저 계산
            int totalProductPrice = orderDetails.stream()
                .mapToInt(OrderDetail::getTotalProductPrice)
                .sum();
            int shippingFee = totalProductPrice >= 20000 ? 0 : 3000;

            // 8. 최종결제금액 계산 (총상품금액 + 배송비)
            int finalAmount = totalProductPrice + shippingFee;

            // 9. 결제수단 매핑
            String paymentMethodName = "알 수 없음";
            if (latestPayment != null && latestPayment.getPaymentMethodId() != null) {
                switch (latestPayment.getPaymentMethodId()) {
                    case "KP" -> paymentMethodName = "카카오페이";
                    case "AC" -> paymentMethodName = "무통장 입금";
                    default -> paymentMethodName = latestPayment.getPaymentMethodId();
                }
            }

            // 10. 모델에 데이터 추가
            model.addAttribute("orderId", order.getOrderId());
            model.addAttribute("orderDate", order.getOrderDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            model.addAttribute("orderStatus", order.getOrderStatus().toString());

            // 주문자 정보
            model.addAttribute("ordererName", user.getUserName());
            model.addAttribute("ordererAddress", user.getUserAddress());
            model.addAttribute("ordererDetailAddress", user.getUserAddressDetail());
            model.addAttribute("ordererPhone", user.getUserPhoneNumber());
            model.addAttribute("ordererEmail", user.getUserEmail());

            // 결제 정보
            model.addAttribute("paymentId", latestPayment != null ? latestPayment.getId().getPaymentId() : "");
            model.addAttribute("paymentMethod", paymentMethodName);
            model.addAttribute("paymentStatus", latestPayment != null ? latestPayment.getPaymentStatus().toString() : "");

            // 상품 정보
            model.addAttribute("productList", productList);
            model.addAttribute("totalProductCount", order.getTotalProductCategory());
            model.addAttribute("totalProductQuantity", order.getTotalProductQuantity());
            model.addAttribute("totalProductPrice", totalProductPrice);
            model.addAttribute("shippingFee", shippingFee);
            model.addAttribute("finalAmount", finalAmount);

            System.out.println("모델 데이터 설정 완료");
            return "order/summary";

        } catch (Exception e) {
            System.err.println("주문 요약 페이지 로드 중 오류: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/main";
        }
    }

    // 카카오페이 결제 준비 API (테스트)
    @PostMapping("/api/kakaopay/ready")
    @ResponseBody
    public ResponseEntity<?> kakaoPayReady(@RequestBody Map<String, Object> payInfo) {
        String url = "https://open-api.kakaopay.com/online/v1/payment/ready";
        Map<String, Object> params = new HashMap<>();
        params.put("cid", cid);
        params.put("partner_order_id", payInfo.getOrDefault("orderNumber", "test_order"));
        params.put("partner_user_id", payInfo.getOrDefault("ordererName", "test_user"));
        params.put("item_name", payInfo.getOrDefault("itemName", "테스트상품"));
        params.put("quantity", payInfo.getOrDefault("quantity", 1));
        params.put("total_amount", payInfo.getOrDefault("totalAmount", 1000));
        params.put("tax_free_amount", 0);
        // 카카오페이 리다이렉트 URL 설정
        params.put("approval_url", "http://localhost:8080/order/payment-success");
        params.put("cancel_url", "http://localhost:8080/order/payment-cancel");
        params.put("fail_url", "http://localhost:8080/order/payment-fail");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "SECRET_KEY " + adminKey);
        headers.set("cid", cid);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(params, headers);
        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "카카오페이 결제 준비 실패: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // 카카오페이 결제 완료 후 처리 페이지
    @GetMapping("/order/payment-success")
    public String paymentSuccess(@RequestParam(required = false) String pg_token, 
                                @RequestParam(required = false) String partner_order_id,
                                Model model, HttpSession session) {
        System.out.println("=== 카카오페이 결제 완료 페이지 진입 ===");
        System.out.println("pg_token: " + pg_token);
        System.out.println("partner_order_id: " + partner_order_id);
        
        // 팝업창에서 부모창에 결제 성공 신호를 보내고 팝업을 닫는 HTML 반환
        model.addAttribute("message", "결제가 완료되었습니다.");
        model.addAttribute("messageType", "KAKAO_PAY_SUCCESS");
        return "order/payment-success";
    }

    // 카카오페이 결제 취소 후 처리 페이지
    @GetMapping("/order/payment-cancel")
    public String paymentCancel(Model model) {
        System.out.println("=== 카카오페이 결제 취소 페이지 진입 ===");
        
        // 팝업창에서 부모창에 결제 취소 신호를 보내고 팝업을 닫는 HTML 반환
        model.addAttribute("message", "결제가 취소되었습니다.");
        model.addAttribute("messageType", "KAKAO_PAY_CANCEL");
        return "order/payment-cancel";
    }

    // 카카오페이 결제 실패 후 처리 페이지
    @GetMapping("/order/payment-fail")
    public String paymentFail(Model model) {
        System.out.println("=== 카카오페이 결제 실패 페이지 진입 ===");
        
        // 팝업창에서 부모창에 결제 실패 신호를 보내고 팝업을 닫는 HTML 반환
        model.addAttribute("message", "결제가 실패했습니다.");
        model.addAttribute("messageType", "KAKAO_PAY_FAIL");
        return "order/payment-fail";
    }
}
