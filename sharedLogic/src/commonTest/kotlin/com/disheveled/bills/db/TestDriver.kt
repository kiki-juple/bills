package com.disheveled.bills.db

import app.cash.sqldelight.db.SqlDriver

/** A fresh in-memory database with the schema already created. */
internal expect fun testDriver(): SqlDriver
