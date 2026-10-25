package com.disheveled.bills.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.inMemoryDriver

internal actual fun testDriver(): SqlDriver = inMemoryDriver(BillsDatabase.Schema)
