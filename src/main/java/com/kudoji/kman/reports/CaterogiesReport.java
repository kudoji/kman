package com.kudoji.kman.reports;

import com.kudoji.kman.Kman;
import com.kudoji.kman.models.Category;
import com.kudoji.kman.models.TransactionType;
import com.kudoji.kman.utils.Strings;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

import static java.lang.String.format;

public class CaterogiesReport extends Report{
    private Category categoryFilter = null;

    public CaterogiesReport() {
        super();
    }

    public CaterogiesReport(LocalDate startDate, LocalDate endDate) {
        this(startDate, endDate, null);
    }

    public CaterogiesReport(LocalDate startDate, LocalDate endDate, Category filter) {
        super(startDate, endDate);
        this.categoryFilter = filter;
    }

    public void setCategoryFilter(Category categoryFilter) {
        this.categoryFilter = categoryFilter;
    }

    public Category getCategroyFilter() {
        return this.categoryFilter;
    }

    @Override
    public void generate(TreeTableView<ReportRow> treeTableView) {
        TreeItem<ReportRow> ttvRoot = prepareTable(treeTableView);

        //  get income first
        String sql;
        if (this.categoryFilter == null){
            //  no filter
            sql = format("select sum(amount_to) as amount, c.name as currency, c.id as currency_id from transactions as t inner join accounts as a on t.account_to_id = a.id inner join currencies as c on a.currencies_id = c.id inner join categories as ct on ct.id = t.categories_id where t.transaction_types_id = %d and (date between '%s' and '%s') group by currency;", TransactionType.ACCOUNT_TYPES_DEPOSIT, this.startDate, this.endDate);
        }else{
            //  there is a filter
            sql = format("select sum(amount_to) as amount, c.name as currency, c.id as currency_id from transactions as t inner join accounts as a on t.account_to_id = a.id inner join currencies as c on a.currencies_id = c.id inner join categories as ct on ct.id = t.categories_id where t.transaction_types_id = %d and (date between '%s' and '%s') and ct.id = %d group by currency;", TransactionType.ACCOUNT_TYPES_DEPOSIT, this.startDate, this.endDate, this.categoryFilter.getId());
        }

//        System.out.println("sql: " + sql);

        List<HashMap<String, String>> rows = Kman.getDB().selectData(sql);
        ttvRoot.getChildren().add(new TreeItem<>(new ReportRow("Total income:", "")));
        for (HashMap<String, String> row: rows){
            ttvRoot.getChildren().add(new TreeItem<>(new ReportRow(format("\t - %s:", row.get("currency")), Strings.userFormat(Float.parseFloat(row.get("amount"))))));

            addRowsToTreeNode(getRowsPerCategoryForCurrencyId(Integer.parseInt(row.get("currency_id")), this.categoryFilter, true),
                    ttvRoot,
                    "\t\tcategories",
                    "\t\t\t");
        }

        //  add empty space between rows
        ttvRoot.getChildren().add(new TreeItem<>(new ReportRow("", "")));

        //  get withdrawal
        if (this.categoryFilter == null){
            //  no filter
            sql = format("select sum(amount_from) as amount, c.name as currency, c.id as currency_id from transactions as t inner join accounts as a on t.account_from_id = a.id inner join currencies as c on a.currencies_id = c.id inner join categories as ct on ct.id = t.categories_id where t.transaction_types_id = %d and (date between '%s' and '%s') group by currency;", TransactionType.ACCOUNT_TYPES_WITHDRAWAL, this.startDate, this.endDate);
        }else{
            //  there is a filter
            sql = format("select sum(amount_from) as amount, c.name as currency, c.id as currency_id from transactions as t inner join accounts as a on t.account_from_id = a.id inner join currencies as c on a.currencies_id = c.id inner join categories as ct on ct.id = t.categories_id where t.transaction_types_id = %d and (date between '%s' and '%s') and ct.id = %d group by currency;", TransactionType.ACCOUNT_TYPES_WITHDRAWAL, this.startDate, this.endDate, this.categoryFilter.getId());
        }

//        System.out.println("sql: " + sql);

        rows = Kman.getDB().selectData(sql);

        ttvRoot.getChildren().add(new TreeItem<>(new ReportRow("Total expenses:", "")));
        for (HashMap<String, String> row: rows){
            ttvRoot.getChildren().add(new TreeItem<>(new ReportRow(format("\t - %s:", row.get("currency")), Strings.userFormat(Float.parseFloat(row.get("amount"))))));

            addRowsToTreeNode(getRowsPerCategoryForCurrencyId(Integer.parseInt(row.get("currency_id")), this.categoryFilter, false),
                    ttvRoot,
                    "\t\tcategories",
                    "\t\t\t");
        }
    }
}
