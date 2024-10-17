package models;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class Pagination {
    String baseUrl;
    List<Integer> pages;
    @Nullable
    String leftPage;
    @Nullable
    String rightPage;

    public static Pagination withTotal(String baseUrl, int page, int totalPages) {
        List<Integer> pages = new ArrayList<>();
        for (int i = Math.max(page - 2, 1); i <= Math.min(page + 2, totalPages); i++) {
            pages.add(i);
        }
        return new Pagination(baseUrl, pages,
            page > 1 ? Integer.toString(page - 1) : null,
            page < totalPages? Integer.toString(page + 1) : null);
    }

    public static Pagination ofUnlimited(String baseUrl, int page) {
        return new Pagination(baseUrl,
            List.of(page),
            page > 1 ? Integer.toString(page - 1) : null,
            Integer.toString(page + 1));
    }
}