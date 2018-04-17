package com.company.salescafe.web.order;

import com.company.salescafe.entity.OrderCard;
import com.company.salescafe.entity.OrderStatus;
import com.company.salescafe.entity.ProductStatus;
import com.company.salescafe.services.OrderService;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.components.*;
import com.company.salescafe.entity.Order;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Map;

public class OrderBrowse extends AbstractLookup {

    @Inject
    private ComponentsFactory componentsFactory;

    @Inject
    protected Messages messages;
    @Inject
    protected GroupTable ordersTable;

    @Inject
    protected OrderService orderService;

    public Component generateOrderCardProductListCell(Order entity) {
        StringBuilder str = new StringBuilder();
        for (OrderCard orderCard : entity.getOrderCard()) {
            if (orderCard.getProduct() != null)
                str.append("[").append(messages.getMessage(ProductStatus.class, "ProductStatus." + orderCard.getProductStatus().name())).append("] ")
                        .append(orderCard.getProduct().getProductName())
                        .append(" x")
                        .append(orderCard.getAmount())
                        .append("\n");
        }
        Label proudctInfoLabel = componentsFactory.createComponent(Label.class);
        proudctInfoLabel.setValue(str.toString());
        return proudctInfoLabel;
    }

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        ordersTable.setStyleProvider((entity, property) -> {
            if (StringUtils.isNotEmpty(property) && "orderStatus".equals(property)) {
                if (OrderStatus.isCompleted.equals(((Order) entity).getOrderStatus()))
                    return "isComplete";
                if (OrderStatus.inWork.equals(((Order) entity).getOrderStatus()))
                    return "inWork";
                if (OrderStatus.isaccepted.equals(((Order) entity).getOrderStatus()))
                    return "isAccepted";
            }

            return null;
        });
    }

    public Component generateActionsCell(Order entity) {
        HBoxLayout hbox = componentsFactory.createComponent(HBoxLayout.class);
        hbox.setSpacing(true);

        final Button passButton = componentsFactory.createComponent(Button.class);
        passButton.setAction(new AbstractAction("orderPassed") {
            @Override
            public void actionPerform(Component component) {
                if (entity.getOrderCard() != null) {
                    int countNotCompletedCards = (int) entity.getOrderCard().stream().filter(o -> !ProductStatus.isComplete.equals(o.getProductStatus())).count();
                    if (countNotCompletedCards > 0) {
                        showNotification(getMessage("notCompletedCardsError"), NotificationType.HUMANIZED);
                        orderService.setOrderStatus(entity, OrderStatus.inWork);
                    } else
                        entity.setOrderStatus(OrderStatus.isCompleted);
                    ordersTable.repaint();
                }
            }
        });
        passButton.setCaption("Выполнен");
        passButton.setStyleName("friendly");
        passButton.setVisible(OrderStatus.inWork.equals(entity.getOrderStatus()));

        final Button acceptButton = componentsFactory.createComponent(Button.class);
        acceptButton.setAction(new AbstractAction("acceptOrder") {
            @Override
            public void actionPerform(Component component) {
                orderService.setOrderStatus(entity, OrderStatus.inWork);
                ordersTable.repaint();
            }
        });
        acceptButton.setCaption("Принять в работу");
        acceptButton.setStyleName("primary");
        acceptButton.setVisible(OrderStatus.isaccepted.equals(entity.getOrderStatus()));

        final Button reopenButton = componentsFactory.createComponent(Button.class);
        reopenButton.setAction(new AbstractAction("reopenOrder") {
            @Override
            public void actionPerform(Component component) {
                orderService.setOrderStatus(entity, OrderStatus.isaccepted);
                ordersTable.repaint();
            }
        });
        reopenButton.setCaption("Выполнить заного");
        reopenButton.setStyleName("danger");
        reopenButton.setVisible(OrderStatus.isCompleted.equals(entity.getOrderStatus()));

        hbox.add(passButton);
        hbox.add(acceptButton);
        hbox.add(reopenButton);
        return hbox;
    }
}