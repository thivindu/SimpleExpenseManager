package lk.ac.mrt.cse.dbs.simpleexpensemanager.control;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import lk.ac.mrt.cse.dbs.simpleexpensemanager.data.AccountDAO;
import lk.ac.mrt.cse.dbs.simpleexpensemanager.data.TransactionDAO;
import lk.ac.mrt.cse.dbs.simpleexpensemanager.data.exception.InvalidAccountException;
import lk.ac.mrt.cse.dbs.simpleexpensemanager.data.impl.PersistentAccountDAO;
import lk.ac.mrt.cse.dbs.simpleexpensemanager.data.impl.PersistentTransactionDAO;
import lk.ac.mrt.cse.dbs.simpleexpensemanager.data.model.Account;
import lk.ac.mrt.cse.dbs.simpleexpensemanager.data.model.ExpenseType;

public class PersistentExpenseManager extends ExpenseManager {

    public PersistentExpenseManager(Context context) {
        super(context);
        setup();
    }

    @Override
    public void setup() {
        /*** Begin generating dummy data for In-Memory implementation ***/

        TransactionDAO persistentTransactionDAO = new PersistentTransactionDAO();
        setTransactionsDAO(persistentTransactionDAO);

        AccountDAO persistentAccountDAO = new PersistentAccountDAO();
        setAccountsDAO(persistentAccountDAO);

        getAccounts();
        try {
            getTransactions();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        /*** End ***/
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDb) {
        sqLiteDb.execSQL("CREATE TABLE `account` (`accountNo` TEXT primary key, `bankName` TEXT NOT NULL, `accountHolderName` TEXT NOT NULL, `balance` REAL NOT NULL)");
        sqLiteDb.execSQL("CREATE TABLE `transaction` (`date` TEXT NOT NULL, `accountNo` TEXT NOT NULL, `expenseType` TEXT NOT NULL, `amount` REAL NOT NULL, FOREIGN KEY(accountNo) REFERENCES account (accountNo))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDb, int i, int i1) {
        sqLiteDb.execSQL("DROP TABLE IF EXISTS `account`");
        sqLiteDb.execSQL("DROP TABLE IF EXISTS `transaction`");
    }

    @Override
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
                getTransactionsDAO().logTransaction(transactionDate, accountNo, expenseType, amountVal);
                updateAccountBalanceDB(accountNo, expenseType, amountVal);
                getAccountsDAO().updateBalance(accountNo, expenseType, amountVal);
            }

        }
    }

    public void addAccount(String accountNo, String bankName, String accountHolderName, double initialBalance) {
        Account account = new Account(accountNo, bankName, accountHolderName, initialBalance);
        addAccountDB(accountNo, bankName, accountHolderName, initialBalance);
        getAccountsDAO().addAccount(account);
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

    private void getAccounts() {
        SQLiteDatabase sqLiteDb = this.getWritableDatabase();
        Cursor cursor = sqLiteDb.rawQuery("SELECT * FROM `account`",null);

        if (cursor.getCount() > 0) {
            while(cursor.moveToNext()){
                Account account = new Account(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getDouble(3));
                getAccountsDAO().addAccount(account);
            }
        }
        cursor.close();

    }

    private void getTransactions() throws ParseException {
        SQLiteDatabase sqLiteDb = this.getWritableDatabase();
        Cursor cursor = sqLiteDb.rawQuery("SELECT * FROM `transaction`",null);

        if (cursor.getCount() > 0) {
            while(cursor.moveToNext()){
                String sDate = cursor.getString(0);
                Date date = new SimpleDateFormat("dd-MM-yyyy").parse(sDate);
                String accountNo = cursor.getString(1);
                String sExpenseType = cursor.getString(2);
                ExpenseType expenseType = ExpenseType.valueOf(sExpenseType);
                double amount = cursor.getDouble(3);
                getTransactionsDAO().logTransaction(date, accountNo, expenseType, amount);
            }
        }
        cursor.close();
    }

}
