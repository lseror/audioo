package com.serortech.audioo.drive

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.DriveScopes

/**
 * GoogleSignIn wrapper scoped to drive.file (only files this app creates).
 * Auth happens by package + SHA-1 fingerprint matched against the OAuth
 * client registered in Google Cloud Console — no client secret embedded.
 */
object DriveAuth {

    private val driveScope = Scope(DriveScopes.DRIVE_FILE)

    fun signInClient(ctx: Context): GoogleSignInClient {
        val opts = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(driveScope)
            .build()
        return GoogleSignIn.getClient(ctx, opts)
    }

    fun lastAccount(ctx: Context): GoogleSignInAccount? =
        GoogleSignIn.getLastSignedInAccount(ctx)

    fun isSignedInWithDrive(ctx: Context): Boolean {
        val account = lastAccount(ctx) ?: return false
        return GoogleSignIn.hasPermissions(account, driveScope)
    }

    fun credential(ctx: Context, account: GoogleSignInAccount): GoogleAccountCredential {
        return GoogleAccountCredential.usingOAuth2(ctx, listOf(DriveScopes.DRIVE_FILE)).apply {
            selectedAccount = account.account
        }
    }
}
