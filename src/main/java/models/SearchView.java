package models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SearchView {
    private String searchText;
    private List<StatsView> statsList;
}
