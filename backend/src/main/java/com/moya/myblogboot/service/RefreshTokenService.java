package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.domain.token.IssuedToken;
import com.moya.myblogboot.domain.token.ReissuedToken;

public interface RefreshTokenService {

    IssuedToken issueOnLogin(Admin admin);

    ReissuedToken rotate(String presentedRefreshToken);

    void revokeOnLogout(String presentedRefreshToken);
}
