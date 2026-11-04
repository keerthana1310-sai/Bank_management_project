package com.bank.service.impl;

import com.bank.dao.AccountDAO;
import com.bank.dao.CustomerDAO;
import com.bank.model.Account;
import com.bank.model.Customer;
import com.bank.service.AccountService;
import com.bank.service.BankingException;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class JdbcAccountService implements AccountService {

    private final AccountDAO accountDAO;
    private final CustomerDAO customerDAO;

        public JdbcAccountService() {
        this.accountDAO = new AccountDAO();
        this.customerDAO = new CustomerDAO();
    }

    @Override
    public Optional<Account> findAccount(String accountNumber)
            throws BankingException {
        try {
            return accountDAO.findByAccountNumber(accountNumber);
        } catch (SQLException e) {
            throw new BankingException("Failed to find account", e);
        }
    }

    @Override
    public List<Account> getAllAccounts() throws BankingException {
        try {
            return accountDAO.findAll();
        } catch (SQLException e) {
            throw new BankingException("Failed to retrieve accounts", e);
        }
    }
}
