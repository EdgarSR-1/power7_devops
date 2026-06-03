package com.springboot.MyTodoList.service.ai;

public final class AiUserContextHolder {

    private static final ThreadLocal<String> currentUserEmail = new ThreadLocal<String>();

    private AiUserContextHolder() {
    }

    public static void set(String email) {
        currentUserEmail.set(email);
    }

    public static String get() {
        return currentUserEmail.get();
    }

    public static void clear() {
        currentUserEmail.remove();
    }
}