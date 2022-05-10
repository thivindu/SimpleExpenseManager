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

import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import lk.ac.mrt.cse.dbs.simpleexpensemanager.control.ExpenseManager;
import lk.ac.mrt.cse.dbs.simpleexpensemanager.control.PersistentExpenseManager;

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

//    @Test
//    public void addTransactionTest() {
//        try {
//            expenseManager.updateAccountBalance("1A", 10, 5, 2022, ExpenseType.EXPENSE, "3413");
//        } catch (InvalidAccountException e) {
//            e.printStackTrace();
//        }
//
//        Calendar calendar = Calendar.getInstance();
//        calendar.set(2022, 5, 10);
//        Date transactionDate = calendar.getTime();
//
//        double amountVal = Double.parseDouble("3413");
//        Transaction transaction = new Transaction(transactionDate, "1A", ExpenseType.EXPENSE, amountVal);
//
//        List<Transaction> transactionList = expenseManager.getTransactionsDAO().getAllTransactionLogs();
//        assertTrue(transactionList.contains(transaction));
//    }
}