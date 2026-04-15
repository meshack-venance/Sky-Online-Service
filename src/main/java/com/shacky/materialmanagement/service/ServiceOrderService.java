package com.shacky.materialmanagement.service;

import com.shacky.materialmanagement.entity.Customer;
import com.shacky.materialmanagement.entity.OnlineService;
import com.shacky.materialmanagement.entity.ServiceOrder;
import com.shacky.materialmanagement.repository.ServiceOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class ServiceOrderService {

    @Autowired
    private ServiceOrderRepository serviceOrderRepository;

    public ServiceOrder saveOrder(ServiceOrder order) {
        return serviceOrderRepository.save(order);
    }

    public List<ServiceOrder> getOrdersByCustomer(Customer customer) {
        return serviceOrderRepository.findByCustomer(customer);
    }

    public List<ServiceOrder> findAll() {
        return serviceOrderRepository.findAll();
    }

    public boolean updateOrderStatus(Long orderId, String status) {
        Optional<ServiceOrder> optionalOrder = serviceOrderRepository.findById(orderId);
        if (optionalOrder.isPresent()) {
            ServiceOrder order = optionalOrder.get();
            order.setStatus(status);
            serviceOrderRepository.save(order);
            return true;
        }
        return false;
    }

    public List<ServiceOrder> getFilteredOrders(String status, Long serviceId) {
        List<ServiceOrder> allOrders = serviceOrderRepository.findAll();
        return allOrders.stream()
                .filter(order -> (status == null || status.isEmpty() || order.getStatus().equalsIgnoreCase(status)))
                .filter(order -> (serviceId == null || order.getService().getId().equals(serviceId)))
                .toList();
    }

    public Optional<ServiceOrder> getOrderById(Long orderId) {
        return serviceOrderRepository.findById(orderId);
    }

    public List<ServiceOrder> getFilteredAndSortedOrders(String status, Long serviceId, String sortBy, String direction) {
        List<ServiceOrder> allOrders = serviceOrderRepository.findAll();

        List<ServiceOrder> filtered = allOrders.stream()
                .filter(order -> (status == null || status.isEmpty() || order.getStatus().equalsIgnoreCase(status)))
                .filter(order -> (serviceId == null || order.getService().getId().equals(serviceId)))
                .toList();

        Comparator<ServiceOrder> comparator;
        switch (sortBy) {
            case "datePlaced":
                comparator = Comparator.comparing(ServiceOrder::getDatePlaced);
                break;
            case "cost":
                comparator = Comparator.comparing(order -> order.getService().getCost());
                break;
            default:
                comparator = Comparator.comparing(ServiceOrder::getId);
                break;
        }

        if ("desc".equalsIgnoreCase(direction)) {
            comparator = comparator.reversed();
        }

        return filtered.stream().sorted(comparator).toList();
    }

    public void deleteOrder(ServiceOrder order) {
        serviceOrderRepository.delete(order);
    }

    /**
     * Creates and persists a new order with status "Pending" for the given customer and service.
     */
    public ServiceOrder createOrder(Customer customer, OnlineService service) {
        ServiceOrder order = new ServiceOrder();
        order.setCustomer(customer);
        order.setService(service);
        order.setStatus("Pending");
        order.setDatePlaced(LocalDateTime.now());
        return serviceOrderRepository.save(order);
    }

    /**
     * Returns the cumulative cost of all existing orders for the customer plus the given service cost.
     */
    public double calculateTotalCostIncluding(Customer customer, OnlineService service) {
        double total = service.getCost() != null ? service.getCost() : 0.0;
        for (ServiceOrder existing : serviceOrderRepository.findByCustomer(customer)) {
            if (existing.getService().getCost() != null) {
                total += existing.getService().getCost();
            }
        }
        return total;
    }

    /**
     * Returns the total cost of all orders for the given customer.
     */
    public double calculateTotalCostForCustomer(Customer customer) {
        return serviceOrderRepository.findByCustomer(customer).stream()
                .mapToDouble(order -> order.getService().getCost() != null ? order.getService().getCost() : 0.0)
                .sum();
    }

    /**
     * Cancels a pending order. Throws IllegalStateException if the order is not in Pending status.
     */
    public void cancelOrder(ServiceOrder order) {
        if (!"Pending".equals(order.getStatus())) {
            throw new IllegalStateException("Only pending orders can be cancelled.");
        }
        serviceOrderRepository.delete(order);
    }
}
