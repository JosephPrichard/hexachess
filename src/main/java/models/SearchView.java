package models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SearchView {
    String searchText;
    List<UserEntity> userList;
    PaginationView pages;
}
