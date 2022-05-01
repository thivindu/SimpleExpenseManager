/*
 * Copyright 2015 Department of Computer Science and Engineering, University of Moratuwa.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *                  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package lk.ac.mrt.cse.dbs.simpleexpensemanager.control;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import lk.ac.mrt.cse.dbs.simpleexpensemanager.control.exception.ExpenseManagerException;
import lk.ac.mrt.cse.dbs.simpleexpensemanager.data.AccountDAO;
import lk.ac.mrt.cse.dbs.simpleexpensemanager.data.TransactionDAO;
import lk.ac.mrt.cse.dbs.simpleexpensemanager.data.exception.InvalidAccountException;
import lk.ac.mrt.cse.dbs.simpleexpensemanager.data.model.Account;
import lk.ac.mrt.cse.dbs.simpleexpensemanager.data.model.ExpenseType;
import lk.ac.mrt.cse.dbs.simpleexpensemanager.data.model.Transaction;

/**
 * The ExpenseManager acts as the mediator when performing transactions. This is an abstract class with an abstract
 * method to setup the DAO objects depending on the implementation.
 */
public abstract class ExpenseManager extends SQLiteOpenHelper implements Serializable {
    private AccountDAO accountsHolder;
    private TransactionDAO transactionsHolder;

    public ExpenseManager(Context context) {
        super(context, "190429G.db", null, 1);
    }

    /***
     * Get list of account numbers as String.
     *
     * @return
     */
    public List<String> getAccountNumbersList() {
        return accountsHolder.getAccountNumbersList();
    }

    /***
     * Update the account balance.
     *
     * @param accountNo
     * @param day
     * @param month
     * @param year
     * @param expenseType
     * @param amount
     * @throws InvalidAccountException
     */
    public void updateAccountBalance(String accountNo, int day, int month, int year, ExpenseType expenseType,
                                     String amount) throws InvalidAccountException {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day);
        Date transactionDate = calendar.getTime();

        if (!amount.isEmpty()) {
            double amountVal = Double.parseDouble(amount);

            if (expenseType == ExpenseType.EXPENSE && !checkAccountBalance(accountNo, amountVal))
                throw new InvalidAccountException("Please enter an amount that is lesser than the account balance.");
            else {
                addTransactionDB(transactionDate, accountNo, expenseType, amountVal);
                transactionsHolder.logTransaction(transactionDate, accountNo, expenseType, amountVal);
                updateAccountBalanceDB(accountNo, expenseType, amountVal);
                accountsHolder.updateBalance(accountNo, expenseType, amountVal);
            }

        }
    }

    /***
     * Get a list of transaction logs.
     *
     * @return
     */
    public List<Transaction> getTransactionLogs() {
        return transactionsHolder.getPaginatedTransactionLogs(10);
    }

    /***
     * Add account to the accounts dao.
     *
     * @param accountNo
     * @param bankName
     * @param accountHolderName
     * @param initialBalance
     */
    public void addAccount(String accountNo, String bankName, String accountHolderName, double initialBalance) {
        Account account = new Account(accountNo, bankName, accountHolderName, initialBalance);
        addAccountDB(accountNo, bankName, accountHolderName, initialBalance);
        accountsHolder.addAccount(account);
    }

    /***
     * Get access to the AccountDAO concrete implementation.
     *
     * @return
     */
    public AccountDAO getAccountsDAO() {
        return accountsHolder;
    }

    /***
     * Set the concrete AccountDAO implementation.
     *
     * @param accountDAO
     */
    public void setAccountsDAO(AccountDAO accountDAO) {
        this.accountsHolder = accountDAO;
    }

    /***
     * Get access to the TransactionDAO concrete implementation.
     *
     * @return
     */
    public TransactionDAO getTransactionsDAO() {
        return transactionsHolder;
    }

    /***
     * Set the concrete TransactionDAO implementation.
     *
     * @param transactionDAO
     */
    public void setTransactionsDAO(TransactionDAO transactionDAO) {
        this.transactionsHolder = transactionDAO;
    }

    private void addAccountDB(String accountNo, String bankName, String accountHolderName, double balance) {
        SQLiteDatabase sqLiteDb = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("accountNo", accountNo);
        contentValues.put("bankName", bankName);
        contentValues.put("accountHolderName", accountHolderName);
        contentValues.put("balance", balance);
        sqLiteDb.insert("account", null, contentValues);
    }

    private void addTransactionDB(Date date, String accountNo, ExpenseType expenseType, double amount) {
        SQLiteDatabase sqLiteDb = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        String strDate = dateFormat.format(date);
        String strExpenseType = expenseType.toString();
        contentValues.put("date", strDate);
        contentValues.put("accountNo", accountNo);
        contentValues.put("expenseType", strExpenseType);
        contentValues.put("amount", amount);
        sqLiteDb.insert("`transaction`", null, contentValues);
    }

    private Boolean checkAccountBalance(String accountNo, double amount) {
        double balance = getAccountBalanceDB(accountNo);

        return !(balance < amount);
    }

    private void updateAccountBalanceDB(String accountNo, ExpenseType expenseType, double amountVal) {
        double curBalance = getAccountBalanceDB(accountNo);
        SQLiteDatabase sqLiteDb = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        double balance = 0.0;

        switch (expenseType) {
            case EXPENSE:
                balance = curBalance - amountVal;
                break;
            case INCOME:
                balance = curBalance + amountVal;
                break;
        }

        contentValues.put("balance", balance);
        Cursor cursor = sqLiteDb.rawQuery("SELECT * FROM `account` WHERE `accountNo` = ?", new String[]{accountNo});

        if (cursor.getCount() > 0)
            sqLiteDb.update("`account`", contentValues, "accountNo=?", new String[]{accountNo});
        cursor.close();

    }

    private double getAccountBalanceDB(String accountNo) {
        SQLiteDatabase sqLiteDb = this.getWritableDatabase();
        Cursor cursor = sqLiteDb.rawQuery("SELECT * FROM `account` WHERE `accountNo` = ?", new String[]{accountNo});
        double balance = 0.0;

        if (cursor.getCount() > 0) {
            while(cursor.moveToNext()){
                balance = cursor.getDouble(3);
            }
        }
        cursor.close();

        return balance;
    }

    /***
     * This method should be implemented by the concrete implementation of this class. It will dictate how the DAO
     * objects will be initialized.
     */
    public abstract void setup() throws ExpenseManagerException;
}
