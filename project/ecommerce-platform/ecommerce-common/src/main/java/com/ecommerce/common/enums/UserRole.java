package com.ecommerce.common.enums;

public enum UserRole {

    /**
     * Standard registered customer.
     * Permissions: register, login, view products, create orders, view own orders.
     */
    USER,

    /**
     * Platform administrator.
     * Permissions: all USER permissions + create/update/delete products,
     *              view all orders, access audit logs.
     */
    ADMIN
}
