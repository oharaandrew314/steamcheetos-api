package io.andrewohara.cheetosbros.api.auth

class FakeAuthorizationDao: AuthorizationDao {
    override fun resolveUserId(token: String) = token
    override fun assignToken(userId: String) = userId
}