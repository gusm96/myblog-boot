package com.moya.myblogboot.service;

import com.moya.myblogboot.dto.visitor.VisitorCountDto;

public interface VisitorCountService {

    VisitorCountDto getVisitorCount(String formattedDate);

    VisitorCountDto incrementVisitorCount(String formattedDate);

}
