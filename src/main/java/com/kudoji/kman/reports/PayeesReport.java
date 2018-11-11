package com.kudoji.kman.reports;

import com.kudoji.kman.Kman;
import com.kudoji.kman.models.Payee;
import com.kudoji.kman.models.TransactionType;
import com.kudoji.kman.utils.Strings;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

import static java.lang.String.format;

public class PayeesReport extends Report {
    private Payee payeeFilter = null;

    public PayeesReport() {
        super();
    }

    public PayeesReport(LocalDate startDate, LocalDate endDate) {
        super(startDate, endDate);
    }

    public PayeesReport(LocalDate startDate, LocalDate endDate, Payee filter) {
        super(startDate, endDate);
        this.payeeFilter = filter;
    }

    public void setPayeeFilter(Payee payeeFilter) {
        this.payeeFilter = payeeFilter;
    }

    public Payee getPayeeFilter() {
        return this.payeeFilter;
    }

    @Override
    public void generate(TreeTableView<ReportRow> treeTableView) {
        TreeItem<ReportRow> ttvRoot = prepareTable(treeTableView);

        //  get income first
        String sql;
        if (this.payeeFilter == null){
            //  no filter
            sql = format("select sum(amount_to) as amount, c.name as currency, c.id as currency_id from transactions as t inner join accounts as a on t.account_to_id = a.id inner join currencies as c on a.currencies_id = c.id inner join payees as p on p.id = t.payees_id where t.transaction_types_id = %d and (date between '%s' and '%s') group by currency;", TransactionType.ACCOUNT_TYPES_DEPOSIT, this.startDate, this.endDate);
        }else{
            //  there is a filter
            sql = format("select sum(amount_to) as amount, c.name as currency, c.id as currency_id from transactions as t inner join accounts as a on t.account_to_id = a.id inner join currencies as c on a.currencies_id = c.id inner join payees as p on p.id = t.payees_id where t.transaction_types_id = %d and (date between '%s' and '%s') and p.id = %d group by currency;", TransactionType.ACCOUNT_TYPES_DEPOSIT, this.startDate, this.endDate, this.payeeFilter.getId());
        }

//        System.out.println("sql: " + sql);

        List<HashMap<String, String>> rows = Kman.getDB().selectData(sql);
        ttvRoot.getChildren().add(new TreeItem<>(new ReportRow("Total income:", "")));
        for (HashMap<String, String> row: rows){
            ttvRoot.getChildren().add(new TreeItem<>(new ReportRow(format("\t - %s:", row.get("currency")), Strings.userFormat(Float.parseFloat(row.get("amount"))))));

            addRowsToTreeNode(getRowsPerPayeeForCurrencyId(Integer.parseInt(row.get("currency_id")), this.payeeFilter, true),
                    ttvRoot,
                    "\t\tpayees",
                    "\t\t\t");
        }

        //  add empty space between rows
        ttvRoot.getChildren().add(new TreeItem<>(new ReportRow("", "")));

        //  get withdrawal
        if (this.payeeFilter == null){
            //  no filter
            sql = format("select sum(amount_from) as amount, c.name as currency, c.id as currency_id from transactions as t inner join accounts as a on t.account_from_id = a.id inner join currencies as c on a.currencies_id = c.id inner join payees as p on p.id = t.payees_id where t.transaction_types_id = %d and (date between '%s' and '%s') group by currency;", TransactionType.ACCOUNT_TYPES_WITHDRAWAL, this.startDate, this.endDate);
        }else{
            //  there is a filter
            sql = format("select sum(amount_from) as amount, c.name as currency, c.id as currency_id from transactions as t inner join accounts as a on t.account_from_id = a.id inner join currencies as c on a.currencies_id = c.id inner join payees as p on p.id = t.payees_id where t.transaction_types_id = %d and (date between '%s' and '%s') and p.id = %d group by currency;", TransactionType.ACCOUNT_TYPES_WITHDRAWAL, this.startDate, this.endDate, this.payeeFilter.getId());
        }

//        System.out.println("sql: " + sql);

        rows = Kman.getDB().selectData(sql);

        ttvRoot.getChildren().add(new TreeItem<>(new ReportRow("Total expenses:", "")));
        for (HashMap<String, String> row: rows){
            ttvRoot.getChildren().add(new TreeItem<>(new ReportRow(format("\t - %s:", row.get("currency")), Strings.userFormat(Float.parseFloat(row.get("amount"))))));

            addRowsToTreeNode(getRowsPerPayeeForCurrencyId(Integer.parseInt(row.get("currency_id")), this.payeeFilter, false),
                    ttvRoot,
                    "\t\tpayees",
                    "\t\t\t");
        }
    }
}