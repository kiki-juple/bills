package com.disheveled.bills

import android.app.Application
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.disheveled.bills.db.BillsDatabase
import com.disheveled.bills.db.BillsStore

class BillsApplication : Application() {

    /**
     * One per process, not per activity: MainActivity is rebuilt on every rotation, and a
     * second driver on the same file is a second connection contending for it.
     */
    val store: BillsStore by lazy {
        BillsStore(AndroidSqliteDriver(BillsDatabase.Schema, this, "bills.db"))
    }
}
