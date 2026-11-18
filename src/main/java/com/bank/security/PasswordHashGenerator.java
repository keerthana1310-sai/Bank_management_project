package com.bank.security;

/** Run with one password argument to print a hash suitable for users.password_hash. */
public final class PasswordHashGenerator {
    private PasswordHashGenerator() { }
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java ... PasswordHashGenerator <password>");
            System.exit(1);
        }
        System.out.println(PasswordUtil.hash(args[0]));
    }
}
