package com.kudoji.kman.reports;

import com.kudoji.kman.Kman;
import com.kudoji.kman.models.Account;
import com.kudoji.kman.models.TransactionType;
import com.kudoji.kman.utils.Strings;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;

import static java.lang.String.format;

public class AccountsReport extends Report {
    private Account accountFilter = null;

    public AccountsReport(){
        super();
    }

    public AccountsReport(LocalDate startDate, LocalDate endDate){
        super(startDate, endDate);
    }

    public AccountsReport(LocalDate startDate, LocalDate endDate, Account filter){
        super(startDate, endDate);
        this.accountFilter = filter;
    }

    public void setAccountFilter(Account accountFilter) {
        this.accountFilter = accountFilter;
    }

    public Account getAccountFilter(){
        return this.accountFilter;
    }

    @Override
    public void generate(TreeTableView<ReportRow> treeTableView){
        TreeItem<ReportRow> ttvRoot = prepareTable(treeTableView);

        LocalDate date = LocalDate.parse(startDate, DateTimeFormatter.ofPattern(dateFormat));
        date = date.minusDays(1);
        String dateString = date.format(DateTimeFormatter.ofPattern(dateFormat));
        ttvRoot.getChildren().add(new TreeItem<>(new ReportRow("Total balance on " + startDate + ":", "")));
        addTotalBalanceOnEndOfTheDay(dateString, ttvRoot, this.accountFilter);
        //  add empty space between rows
        ttvRoot.getChildren().add(new TreeItem<>(new ReportRow("", "")));

        ttvRoot.getChildren().add(new TreeItem<>(new ReportRow("Total balance on " + endDate + ":", "")));
        addTotalBalanceOnEndOfTheDay(endDate, ttvRoot, this.accountFilter);
        //  add empty space between rows
        ttvRoot.getChildren().add(new TreeItem<>(new ReportRow("", "")));

        //  get income first
        String sql;
        if (this.accountFilter == null){
            //  no filter
            sql = format("select sum(amount_to) as amount, c.name as currency, c.id as currency_id from transactions as t inner join accounts as a on t.account_to_id = a.id inner join currencies as c on a.currencies_id = c.id where t.transaction_types_id = %d and (date between '%s' and '%s') group by currency;", TransactionType.ACCOUNT_TYPES_DEPOSIT, this.startDate, this.endDate);
        }else{
            //  there is a filter
            sql = format("select sum(amount_to) as amount, c.name as currency, c.id as currency_id from transactions as t inner join accounts as a on t.account_to_id = a.id inner join currencies as c on a.currencies_id = c.id where t.transaction_types_id = %d and (date between '%s' and '%s') and a.id = %d group by currency;", TransactionType.ACCOUNT_TYPES_DEPOSIT, this.startDate, this.endDate, this.accountFilter.getId());
        }

        List<HashMap<String, String>> rows = Kman.getDB().selectData(sql);
        ttvRoot.getChildren().add(new TreeItem<>(new ReportRow("Total income:", "")));
        for (HashMap<String, String> row: rows){
            ttvRoot.getChildren().add(new TreeItem<>(new ReportRow(format("\t - %s:", row.get("currency")), Strings.userFormat(Float.parseFloat(row.get("amount"))))));

            addRowsToTreeNode(getRowsPerAccountForCurrencyId(Integer.parseInt(row.get("currency_id")), this.accountFilter, true),
                    ttvRoot,
                    "\t\taccounts",
                    "\t\t\t");
        }

        //  add empty space between rows
        ttvRoot.getChildren().add(new TreeItem<>(new ReportRow("", "")));

        //  get withdrawal
        if (this.accountFilter == null){
            //  no filter
            sql = format("select sum(amount_from) as amount, c.name as currency, c.id as currency_id from transactions as t inner join accounts as a on t.account_from_id = a.id inner join currencies as c on a.currencies_id = c.id where t.transaction_types_id = %d and (date between '%s' and '%s') group by currency;", TransactionType.ACCOUNT_TYPES_WITHDRAWAL, this.startDate, this.endDate);
        }else{
            //  there is a filter
            sql = format("select sum(amount_from) as amount, c.name as currency, c.id as currency_id from transactions as t inner join accounts as a on t.account_from_id = a.id inner join currencies as c on a.currencies_id = c.id where t.transaction_types_id = %d and (date between '%s' and '%s') and a.id = %d group by currency;", TransactionType.ACCOUNT_TYPES_WITHDRAWAL, this.startDate, this.endDate, this.accountFilter.getId());
        }

        rows = Kman.getDB().selectData(sql);

        ttvRoot.getChildren().add(new TreeItem<>(new ReportRow("Total expenses:", "")));
        for (HashMap<String, String> row: rows){
            ttvRoot.getChildren().add(new TreeItem<>(new ReportRow(format("\t - %s:", row.get("currency")), Strings.userFormat(Float.parseFloat(row.get("amount"))))));

            addRowsToTreeNode(getRowsPerAccountForCurrencyId(Integer.parseInt(row.get("currency_id")), this.accountFilter, false),
                    ttvRoot,
                    "\t\taccounts",
                    "\t\t\t");
        }
    }
}
