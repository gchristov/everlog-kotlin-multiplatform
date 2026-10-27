package com.everlog.managers.auth

import com.everlog.data.model.ELUser

/**
 * The user saved on this phone when it's the one signed in, to carry on with at launch without waiting for their
 * document: offline with nothing cached, the document can't load, which would log them out. Null when it isn't
 * theirs or there's none, as the app can't run without a user.
 */
internal fun savedUserFor(signedInUid: String, saved: ELUser?): ELUser? = saved?.takeIf { it.id == signedInUid }
