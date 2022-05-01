package lk.ac.mrt.cse.dbs.simpleexpensemanager.control;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import lk.ac.mrt.cse.dbs.simpleexpensemanager.data.AccountDAO;
import lk.ac.mrt.cse.dbs.simpleexpensemanager.data.TransactionDAO;
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
