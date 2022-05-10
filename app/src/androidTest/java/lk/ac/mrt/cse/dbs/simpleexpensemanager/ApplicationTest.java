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

package lk.ac.mrt.cse.dbs.simpleexpensemanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import lk.ac.mrt.cse.dbs.simpleexpensemanager.control.ExpenseManager;
import lk.ac.mrt.cse.dbs.simpleexpensemanager.control.PersistentExpenseManager;
import lk.ac.mrt.cse.dbs.simpleexpensemanager.data.exception.InvalidAccountException;
import lk.ac.mrt.cse.dbs.simpleexpensemanager.data.model.ExpenseType;
import lk.ac.mrt.cse.dbs.simpleexpensemanager.data.model.Transaction;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest {
    private ExpenseManager expenseManager;

    @Before
    public void createExpenseManager() {
        Context context = ApplicationProvider.getApplicationContext();
        expenseManager = new PersistentExpenseManager(context);
    }

    @Test
    public void addAccountTest() {
        expenseManager.addAccount("2B","BBank","BBB", 5000);
        List<String> accountNumbers = expenseManager.getAccountNumbersList();
        assertTrue(accountNumbers.contains("2B"));
    }

    @Test
    public void addTransactionTest() {
        try {
            expenseManager.updateAccountBalance("6F", 28, 3, 3014, ExpenseType.INCOME, "3000000");
        } catch (InvalidAccountException e) {
            e.printStackTrace();
        }

        List<Transaction> transactionList = expenseManager.getTransactionsDAO().getAllTransactionLogs();
        Transaction insertedTransaction = transactionList.get(transactionList.size()-1);

        int year = 3014;
        int month = 4;
        int day = 28;

        Date transactionDate = insertedTransaction.getDate();
        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        String strDate = dateFormat.format(transactionDate);
        String expectedDate = String.valueOf(day)+"-"+String.format("%02d", month)+"-"+String.valueOf(year);

        assertEquals("6F", insertedTransaction.getAccountNo());
        assertEquals(expectedDate, strDate);
        assertEquals(ExpenseType.INCOME, insertedTransaction.getExpenseType());
        assertEquals(3000000.0, insertedTransaction.getAmount(), 0.0);
    }
}