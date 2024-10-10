package models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class LeaderboardView {
    List<UserEntity> userList;
    PaginationView pages;
}