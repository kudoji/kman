package com.kudoji.kman.reports;

import com.kudoji.kman.Kman;
import com.kudoji.kman.models.Account;
import com.kudoji.kman.models.TransactionType;
import com.kudoji.kman.utils.Strings;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

/**
 * Generate statistics information
 */
public class StatsReport extends Report{
    public StatsReport(){
        super();
    }

    public StatsReport(LocalDate startDate, LocalDate endDate){
        super(startDate, endDate);
    }

    @Override
    public void generate(TreeTableView<ReportRow> treeTableView){
        TreeItem<ReportRow> ttvRoot = prepareTable(treeTableView);

        LocalDate date = LocalDate.parse(startDate, DateTimeFormatter.ofPattern(dateFormat));
        date = date.minusDays(1);
        String dateString = date.format(DateTimeFormatter.ofPattern(dateFormat));
        ttvRoot.getChildren().add(new TreeItem<>(new ReportRow("Total balance on " + startDate + ":", "")));
        addTotalBalanceOnEndOfTheDay(dateString, ttvRoot, null);
        //  add empty space between rows
        ttvRoot.getChildren().add(new TreeItem<>(new ReportRow("", "")));

        ttvRoot.getChildren().add(new TreeItem<>(new ReportRow("Total balance on " + endDate + ":", "")));
        addTotalBalanceOnEndOfTheDay(endDate, ttvRoot, null);
        //  add empty space between rows
        ttvRoot.getChildren().add(new TreeItem<>(new ReportRow("", "")));

        //  get income first
        String sql = format("select sum(amount_to) as amount, c.name as currency, c.id as currency_id from transactions as t inner join accounts as a on t.account_to_id = a.id inner join currencies as c on a.currencies_id = c.id where t.transaction_types_id = %d and (date between '%s' and '%s') group by currency;", TransactionType.ACCOUNT_TYPES_DEPOSIT, this.startDate, this.endDate);
        List<HashMap<String, String>> rows = Kman.getDB().selectData(sql);
        ttvRoot.getChildren().add(new TreeItem<>(new ReportRow("Total income:", "")));
        for (HashMap<String, String> row: rows){
            ttvRoot.getChildren().add(new TreeItem<>(new ReportRow(format("\t - %s:", row.get("currency")), Strings.userFormat(Float.parseFloat(row.get("amount"))))));

            addRowsToTreeNode(getRowsPerAccountForCurrencyId(Integer.parseInt(row.get("currency_id")), null, true),
                    ttvRoot,
                    "\t\taccounts",
                    "\t\t\t");
            addRowsToTreeNode(getRowsPerPayeeForCurrencyId(Integer.parseInt(row.get("currency_id")), true),
                    ttvRoot,
                    "\t\tpayees",
                    "\t\t\t");
            addRowsToTreeNode(getRowsPerCategoryForCurrencyId(Integer.parseInt(row.get("currency_id")), true),
                    ttvRoot,
                    "\t\tcategories",
                    "\t\t\t");
        }

        //  add empty space between rows
        ttvRoot.getChildren().add(new TreeItem<>(new ReportRow("", "")));

        //  get withdrawal
        sql = format("select sum(amount_from) as amount, c.name as currency, c.id as currency_id from transactions as t inner join accounts as a on t.account_from_id = a.id inner join currencies as c on a.currencies_id = c.id where t.transaction_types_id = %d and (date between '%s' and '%s') group by currency;", TransactionType.ACCOUNT_TYPES_WITHDRAWAL, this.startDate, this.endDate);
        rows = Kman.getDB().selectData(sql);

        ttvRoot.getChildren().add(new TreeItem<>(new ReportRow("Total expenses:", "")));
        for (HashMap<String, String> row: rows){
            ttvRoot.getChildren().add(new TreeItem<>(new ReportRow(format("\t - %s:", row.get("currency")), Strings.userFormat(Float.parseFloat(row.get("amount"))))));

            addRowsToTreeNode(getRowsPerAccountForCurrencyId(Integer.parseInt(row.get("currency_id")), null, false),
                    ttvRoot,
                    "\t\taccounts",
                    "\t\t\t");
            addRowsToTreeNode(getRowsPerPayeeForCurrencyId(Integer.parseInt(row.get("currency_id")), false),
                    ttvRoot,
                    "\t\tpayees",
                    "\t\t\t");
            addRowsToTreeNode(getRowsPerCategoryForCurrencyId(Integer.parseInt(row.get("currency_id")), false),
                    ttvRoot,
                    "\t\tcategories",
                    "\t\t\t");
        }
    }
}