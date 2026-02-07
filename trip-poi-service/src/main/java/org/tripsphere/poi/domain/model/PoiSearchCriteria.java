package org.tripsphere.poi.domain.model;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PoiSearchCriteria {
    private List<String> categories;
    private String adcode;
}
