/*
package com.moya.myblogboot.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminAuthCheckInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        HttpSession session = request.getSession(false);
        if(session != null && session.getAttribute("admin") != null){
            return true;
        }
        String url = request.getRequestURL().toString();
        response.sendRedirect(request.getContextPath()+"/login/admin?referer="+url);
        return false;
    }

}
*/
