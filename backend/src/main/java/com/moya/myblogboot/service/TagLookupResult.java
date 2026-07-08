package com.moya.myblogboot.service;

import com.moya.myblogboot.dto.tag.TagResDto;

public sealed interface TagLookupResult permits TagLookupResult.Direct, TagLookupResult.Redirect {
    record Direct(TagResDto tag) implements TagLookupResult {
    }

    record Redirect(String targetSlug) implements TagLookupResult {
    }
}
