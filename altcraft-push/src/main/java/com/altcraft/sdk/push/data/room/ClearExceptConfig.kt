package com.altcraft.sdk.push.data.room

//  Created by Andrey Pogodin.
//
//  Copyright © 2026 Altcraft. All rights reserved.
//

import androidx.room.withTransaction

/**
 * Clears all push module database tables except the configuration table.
 */
internal suspend fun DB.clearExceptConfig() = withTransaction {
    val db = openHelper.writableDatabase

    db.query(
        """
        SELECT name FROM sqlite_master
        WHERE type = 'table'
        AND name NOT LIKE 'sqlite_%'
        AND name != 'room_master_table'
        AND name != 'push_config_table'
        """.trimIndent()
    ).use { cursor ->
        while (cursor.moveToNext()) {
            val table = cursor
                .getString(0)
                .replace("\"", "\"\"")
            db.execSQL("DELETE FROM \"$table\"")
        }
    }
}