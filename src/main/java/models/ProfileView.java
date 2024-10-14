package models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ProfileView {
    UserEntity user;
    List<HistoryEntity> historyList;
}
