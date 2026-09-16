package com.example.ecommerce.cart.service;

import com.example.ecommerce.cart.dto.OrderResponseDTO;
import com.example.ecommerce.cart.entity.*;
import com.example.ecommerce.common.exception.*;
import com.example.ecommerce.cart.repository.CartRepository;
import com.example.ecommerce.cart.dto.CartItemDTO;
import com.example.ecommerce.cart.dto.CartResponseDTO;
import com.example.ecommerce.cart.repository.OrderRepository;
import com.example.ecommerce.cart.repository.ProductRepository;
import com.example.ecommerce.cart.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class CartService {
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;

    public CartService(UserRepository userRepository, ProductRepository productRepository, CartRepository cartRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
    }


    private List<CartItemDTO> convertItemToDTO(List<CartItems> items){
        return items.stream().map(CartItemDTO::new).toList();
    }

    private BigDecimal calculateTotalPrice(Cart cart){
        BigDecimal totalprice = BigDecimal.ZERO;
        for (CartItems item : cart.getCartItems()) {
            BigDecimal subTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalprice = totalprice.add(subTotal);
        }
        return totalprice;
    }

    public CartResponseDTO getCart(Long userId){
        User user = userRepository.findById(userId).orElseThrow(()-> new UserNotFoundException("user not found."));
        Cart cart = user.getCart();
        if(cart == null){
            throw new CartNotFoundException("Cart not found.");
        }
        return new CartResponseDTO(cart.getId(),user.getUserId(), convertItemToDTO(cart.getCartItems()));

    }




   @Transactional
    public List<CartItemDTO> addToCart(Long userId, Long productId, Long quantity) {
       if (quantity < 1) {
           throw new InvalidQuantity("Quantity must be greater than zero.");
       }

       User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("user not found."));
       Product product = productRepository.findById(productId).orElseThrow(() -> new ProductNotFoundException("product not found."));
       Cart cart = user.getCart();
       if (cart == null) {
           cart = new Cart();
           cart.setUser(user);
           user.setCart(cart);
       }

       CartItems existingItem = null;
       for (CartItems item : cart.getCartItems()) {
           if (item.getProduct().getId().equals(product.getId())) {
               existingItem = item;
               break;
           }
       }

       Long currentQuantity = existingItem != null? existingItem.getQuantity():0L;
       Long  newQuantity = currentQuantity + quantity;

       if(newQuantity > product.getStock()){
           throw new OutOfStockException("Product out of stock.");
       }

           if (existingItem != null) {
               existingItem.setQuantity(newQuantity);
           } else {
               CartItems newItem = new CartItems(product, newQuantity);
               newItem.setCart(cart);
               cart.getCartItems().add(newItem);
           }

           BigDecimal newTotalPrice = calculateTotalPrice(cart);
           cart.setTotal(newTotalPrice);
       cartRepository.save(cart);
        return convertItemToDTO(cart.getCartItems());
    }


    @Transactional
    public void removeFromCart(Long userId, Long itemId){
        User user = userRepository.findById(userId).orElseThrow(()-> new UserNotFoundException("user not found."));
        Cart cart = user.getCart();
        if(cart == null){
            throw new CartNotFoundException("Cart not found.");
        }

        boolean removed = cart.getCartItems().removeIf(item-> itemId.equals(item.getId()));
        if(!removed){
          throw new CartItemNotFoundException("Cart item not found.");
        }
            cart.setTotal(calculateTotalPrice(cart));
            cartRepository.save(cart);
    }


    public List<CartItemDTO> changeQuantity(Long userId, Long cartItemId, Long change) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("user not found."));
        Cart cart = user.getCart();
        if(cart == null)
            throw new CartNotFoundException("Cart not found.");

        if(cart.getCartItems().isEmpty())
            throw new EmptyCartException("Cart is Empty.");

        boolean found =false;
        for(CartItems item : cart.getCartItems()) {
            if (item.getId().equals(cartItemId)) {
                long newQuantity = item.getQuantity() + change;
                if(newQuantity < 1){
                    throw new InvalidQuantity("Quantity cannot be less than 1.");
                }
                if(newQuantity > item.getProduct().getStock()){
                    throw new OutOfStockException("Out of stock.");
                }
                item.setQuantity(newQuantity);
                found = true;
                break;
            }
        }
        if(!found)
            throw new CartItemNotFoundException("Item not found.");
        cart.setTotal(calculateTotalPrice(cart));
        cartRepository.save(cart);
        return convertItemToDTO(cart.getCartItems());
    }


    public List<CartItemDTO> updateQuantity(Long userId, Long cartItemId, Long quantity) {
        if (quantity < 1) {
            throw new InvalidQuantity("Quantity must be greater than zero.");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("user not found."));
        Cart cart = user.getCart();
        if(cart == null)
            throw new CartNotFoundException("Cart not found.");

        if(cart.getCartItems().isEmpty())
            throw new EmptyCartException("Cart is Empty.");

        boolean found =false;
        for(CartItems item : cart.getCartItems()) {
            if (item.getId().equals(cartItemId)) {
                if(quantity > item.getProduct().getStock()){
                 throw new OutOfStockException("Out of stock.");
                }
                item.setQuantity(quantity);
                found = true;
                break;
            }
        }
        if(!found)
            throw new CartItemNotFoundException("Item not found.");

        cart.setTotal(calculateTotalPrice(cart));
        cartRepository.save(cart);
        return convertItemToDTO(cart.getCartItems());
    }

    @Transactional
    public void clearCart(Long userId){
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("user not found."));
        Cart cart = user.getCart();
        if(cart == null)
            throw new CartNotFoundException("Cart not found.");
        cart.getCartItems().clear();
        cart.setTotal(BigDecimal.ZERO);
        cartRepository.save(cart);
    }



    private OrderItem convertCartToOrder(CartItems item){
        return new OrderItem(item);
    }

    //ORDER ACTION


    @Transactional
    public OrderResponseDTO order(Long userId){
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("user not found."));
        Cart cart = user.getCart();

        if(cart == null || cart.getCartItems() == null || cart.getCartItems().isEmpty()){ throw new EmptyCartException("Cart is empty."); }

        Order order = new Order();
        order.setUser(user);
        order.setStatus(Order.OrderStatus.PENDING);

        for(CartItems item : cart.getCartItems()){
            if(item.getQuantity() > item.getProduct().getStock())
                throw new OutOfStockException("Product out of stock.");
            OrderItem orderItem = convertCartToOrder(item);
            item.getProduct().setStock(item.getProduct().getStock() - item.getQuantity());
            orderItem.setOrder(order);
            order.getOrderItems().add(orderItem);

         }


        //BigDecimal totalPrice = cart.getCartItems().stream().map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal totalPrice = calculateTotalPrice(cart);
        order.setTotalAmount(totalPrice);

        Order savedOrder = orderRepository.save(order);
        cart.getCartItems().clear();
        cart.setTotal(BigDecimal.ZERO);
        cartRepository.save(cart);
        return new OrderResponseDTO(savedOrder);
    }

 }