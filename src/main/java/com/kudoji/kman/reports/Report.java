package com.kudoji.kman.reports;

import com.kudoji.kman.Kman;
import com.kudoji.kman.models.Account;
import com.kudoji.kman.models.Category;
import com.kudoji.kman.models.Payee;
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

public abstract class Report {
    //  format for dates
    final String dateFormat = "yyyy-MM-dd";
    //  LocalDate converted to String using dateFormat
    String startDate, endDate;

    public Report(){
        this.startDate = LocalDate.now().format(DateTimeFormatter.ofPattern(dateFormat));
        this.endDate = this.startDate;
    }

    public Report(LocalDate startDate, LocalDate endDate){
        this.startDate = startDate.format(DateTimeFormatter.ofPattern(dateFormat));
        this.endDate = endDate.format(DateTimeFormatter.ofPattern(dateFormat));
    }

    public void setStartDate(LocalDate startDate){
        this.startDate = startDate.format(DateTimeFormatter.ofPattern(dateFormat));
    }

    public String getStartDate(){
        return this.startDate;
    }

    public void setEndDate(LocalDate endDate){
        this.endDate = endDate.format(DateTimeFormatter.ofPattern(dateFormat));
    }

    public String getEndDate(){
        return this.endDate;
    }

    public String getDateFormat(){
        return this.dateFormat;
    }

    /**
     * Prepares TreeTableView
     *
     * @param treeTableView
     * @return
     */
    TreeItem<ReportRow> prepareTable(TreeTableView<ReportRow> treeTableView){
        TreeItem<ReportRow> ttvRoot;
        if (treeTableView.getRoot() != null){
            treeTableView.getRoot().getChildren().clear();
            ttvRoot = treeTableView.getRoot();
        }else{
            //  root is not set thus, columns are too
            ttvRoot = new TreeItem<>(new ReportRow("Report", ""));

            TreeTableColumn<ReportRow, String> ttcParameter = new TreeTableColumn<>("Parameter");
            ttcParameter.setPrefWidth(450);
            ttcParameter.setCellValueFactory((TreeTableColumn.CellDataFeatures<ReportRow, String> param) ->
                    new ReadOnlyStringWrapper(param.getValue().getValue().getParameter()));

            TreeTableColumn<ReportRow, String> ttcValue = new TreeTableColumn<>("Value");
            ttcValue.setPrefWidth(150);
            ttcValue.setResizable(false);
            ttcValue.setStyle("-fx-alignment: center-right;");
            ttcValue.setCellValueFactory((TreeTableColumn.CellDataFeatures<ReportRow, String> param) ->
                    new ReadOnlyStringWrapper(param.getValue().getValue().getValue()));

            treeTableView.setRoot(ttvRoot);
            treeTableView.setShowRoot(false);
            treeTableView.getColumns().setAll(ttcParameter, ttcValue);
            ttvRoot.setExpanded(true);
        }

        return ttvRoot;
    }

    public abstract void generate(TreeTableView<ReportRow> treeTableView);

    /**
     * Adds total balances per each currency for the end of the day of the date
     *
     * @param date
     * @param treeNode
     * @param filter null - take all accounts or particular value to filter on
     */
    void addTotalBalanceOnEndOfTheDay(String date, TreeItem<ReportRow> treeNode, Account filter){
        //  keeps balance per currency
        Map<String, BigDecimal> currencies = new HashMap<>();
        String currencyName = "";
        BigDecimal balance = null;

        if (filter == null){
            //  take all accounts
            List<Account> accounts = Account.getAccounts();
            for (Account account: accounts){
                currencyName = account.getCurrency().getName();
                balance = currencies.get(currencyName);
                if (balance != null){
                    currencies.put(currencyName, balance.add(account.getBalanceDate(date, -1)));
                }else{
                    currencies.put(currencyName, account.getBalanceDate(date, -1));
                }
            }
        }else{
            //  take only the one
            currencyName = filter.getCurrency().getName();

            currencies.put(currencyName, filter.getBalanceDate(date, -1));
        }

        for (Map.Entry<String, BigDecimal> currency: currencies.entrySet()){
            treeNode.getChildren().add(
                    new TreeItem<>(
                            new ReportRow(
                                    "\t" + currency.getKey() + ":",
                                    Strings.userFormat(currency.getValue().floatValue())
                            )
                    ));
        }
    }

    /**
     * Adds rows into TreeItem
     *
     * @param rows rows to be added to
     * @param treeNode where to add
     * @param nodeName name of node that will be created
     * @param prefix will be added to text
     */
    void addRowsToTreeNode(List<HashMap<String, String>> rows, TreeItem<ReportRow> treeNode, String nodeName, String prefix){
        if (!rows.isEmpty()){
            TreeItem<ReportRow> ttvInnerRoot;

            ttvInnerRoot = new TreeItem<>(new ReportRow(nodeName, ""));
            ttvInnerRoot.setExpanded(false);
            for (HashMap<String, String> innerRow: rows){
                ttvInnerRoot.getChildren().add(
                        new TreeItem<>(
                                new ReportRow(prefix + innerRow.get("parameter"), Strings.userFormat(Float.parseFloat(innerRow.get("value"))))
                        )
                );
            }

            treeNode.getChildren().add(ttvInnerRoot);
        }
    }

    /**
     * Returns DB rows per each account for particular currency id
     *
     * @param currencyId take transaction records for the selected currency only
     * @param accountFilter null - no filter
     * @param isDeposit true - return records for deposit, false for withdrawal
     *
     * @return DB records
     */
    List<HashMap<String, String>> getRowsPerAccountForCurrencyId(int currencyId, Account accountFilter, boolean isDeposit){
        String sql = "";
        if (isDeposit){
            if (accountFilter == null){
                sql = format("select sum(amount_to) as value, a.name as parameter from transactions as t inner join accounts as a on t.account_to_id = a.id where a.currencies_id = %d and t.transaction_types_id = %d and (date between '%s' and '%s') group by parameter order by value desc;", currencyId, TransactionType.ACCOUNT_TYPES_DEPOSIT, this.startDate, this.endDate);

            }else{
                sql = format("select sum(amount_to) as value, a.name as parameter from transactions as t inner join accounts as a on t.account_to_id = a.id where a.currencies_id = %d and t.transaction_types_id = %d and (date between '%s' and '%s') and a.id = %d group by parameter order by value desc;", currencyId, TransactionType.ACCOUNT_TYPES_DEPOSIT, this.startDate, this.endDate, accountFilter.getId());
            }
        }else{
            if (accountFilter == null){
                sql = format("select sum(amount_from) as value, a.name as parameter from transactions as t inner join accounts as a on t.account_from_id = a.id where a.currencies_id = %d and t.transaction_types_id = %d and (date between '%s' and '%s') group by parameter order by value desc;", currencyId, TransactionType.ACCOUNT_TYPES_WITHDRAWAL, this.startDate, this.endDate);
            }else{
                sql = format("select sum(amount_from) as value, a.name as parameter from transactions as t inner join accounts as a on t.account_from_id = a.id where a.currencies_id = %d and t.transaction_types_id = %d and (date between '%s' and '%s') and a.id = %d group by parameter order by value desc;", currencyId, TransactionType.ACCOUNT_TYPES_WITHDRAWAL, this.startDate, this.endDate, accountFilter.getId());
            }
        }

//        System.out.println("sql: " + sql);

        return Kman.getDB().selectData(sql);
    }

    /**
     * Returns DB rows per each payee for particular currency id
     *
     * @param currencyId take transaction records for the selected currency only
     * @param payeeFilter null or value to be filtered
     * @param isDeposit true - return records for deposit, false for withdrawal
     *
     * @return DB records
     */
    List<HashMap<String, String>> getRowsPerPayeeForCurrencyId(int currencyId, Payee payeeFilter, boolean isDeposit){
        String sql = "";
        if (isDeposit){
            if (payeeFilter == null){
                sql = format("select sum(amount_to) as value, p.name as parameter from transactions as t inner join accounts as a on t.account_to_id = a.id inner join payees as p on p.id = t.payees_id where a.currencies_id = %d and t.transaction_types_id = %d and (date between '%s' and '%s') group by parameter order by value desc;", currencyId, TransactionType.ACCOUNT_TYPES_DEPOSIT, this.startDate, this.endDate);
            }else{
                sql = format("select sum(amount_to) as value, p.name as parameter from transactions as t inner join accounts as a on t.account_to_id = a.id inner join payees as p on p.id = t.payees_id where a.currencies_id = %d and t.transaction_types_id = %d and (date between '%s' and '%s') and p.id = %d group by parameter order by value desc;", currencyId, TransactionType.ACCOUNT_TYPES_DEPOSIT, this.startDate, this.endDate, payeeFilter.getId());
            }
        }else{
            if (payeeFilter == null){
                sql = format("select sum(amount_from) as value, p.name as parameter from transactions as t inner join accounts as a on t.account_from_id = a.id inner join payees as p on p.id = t.payees_id where a.currencies_id = %d and t.transaction_types_id = %d and (date between '%s' and '%s') group by parameter order by value desc;", currencyId, TransactionType.ACCOUNT_TYPES_WITHDRAWAL, this.startDate, this.endDate);
            }else{
                sql = format("select sum(amount_from) as value, p.name as parameter from transactions as t inner join accounts as a on t.account_from_id = a.id inner join payees as p on p.id = t.payees_id where a.currencies_id = %d and t.transaction_types_id = %d and (date between '%s' and '%s') and p.id = %d group by parameter order by value desc;", currencyId, TransactionType.ACCOUNT_TYPES_WITHDRAWAL, this.startDate, this.endDate, payeeFilter.getId());
            }
        }

//        System.out.println("sql: " + sql);

        return Kman.getDB().selectData(sql);
    }

    /**
     * Returns DB rows per each category for particular currency id
     *
     * @param currencyId take transaction records for the selected currency only
     * @param categoryFilter null or value date to be filtered on
     * @param isDeposit true - return records for deposit, false for withdrawal
     *
     * @return DB records
     */
    List<HashMap<String, String>> getRowsPerCategoryForCurrencyId(int currencyId, Category categoryFilter, boolean isDeposit){
        String sql = "";
        if (isDeposit){
            if (categoryFilter == null){
                sql = format("select sum(amount_to) as value, c.name as parameter from transactions as t inner join accounts as a on t.account_to_id = a.id inner join categories as c on c.id = t.categories_id where a.currencies_id = %d and t.transaction_types_id = %d and (date between '%s' and '%s') group by parameter order by value desc;", currencyId, TransactionType.ACCOUNT_TYPES_DEPOSIT, this.startDate, this.endDate);
            }else{
                sql = format("select sum(amount_to) as value, c.name as parameter from transactions as t inner join accounts as a on t.account_to_id = a.id inner join categories as c on c.id = t.categories_id where a.currencies_id = %d and t.transaction_types_id = %d and (date between '%s' and '%s') and c.id = %d group by parameter order by value desc;", currencyId, TransactionType.ACCOUNT_TYPES_DEPOSIT, this.startDate, this.endDate, categoryFilter.getId());
            }
        }else{
            if (categoryFilter == null){
                sql = format("select sum(amount_from) as value, c.name as parameter from transactions as t inner join accounts as a on t.account_from_id = a.id inner join categories as c on c.id = t.categories_id where a.currencies_id = %d and t.transaction_types_id = %d and (date between '%s' and '%s') group by parameter order by value desc;", currencyId, TransactionType.ACCOUNT_TYPES_WITHDRAWAL, this.startDate, this.endDate);
            }else{
                sql = format("select sum(amount_from) as value, c.name as parameter from transactions as t inner join accounts as a on t.account_from_id = a.id inner join categories as c on c.id = t.categories_id where a.currencies_id = %d and t.transaction_types_id = %d and (date between '%s' and '%s') and c.id = %d group by parameter order by value desc;", currencyId, TransactionType.ACCOUNT_TYPES_WITHDRAWAL, this.startDate, this.endDate, categoryFilter.getId());
            }
        }

//        System.out.println("sql: " + sql);

        return Kman.getDB().selectData(sql);
    }
}