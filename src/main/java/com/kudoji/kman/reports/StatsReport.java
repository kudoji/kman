package com.kudoji.kman.reports;

import com.kudoji.kman.Kman;
import com.kudoji.kman.models.Account;
import com.kudoji.kman.models.TransactionType;
import com.kudoji.kman.utils.Strings;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
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
public class StatsReport {
    private String startDate, endDate;
    private final String dateFormat = "yyyy-MM-dd";

    public StatsReport(){
        this.startDate = LocalDate.now().format(DateTimeFormatter.ofPattern(dateFormat));
        this.endDate = this.startDate;
    }

    public StatsReport(LocalDate startDate, LocalDate endDate){
        this.startDate = startDate.format(DateTimeFormatter.ofPattern(dateFormat));
        this.endDate = endDate.format(DateTimeFormatter.ofPattern(dateFormat));
    }

    public void setStartDate(LocalDate startDate){
        this.startDate = startDate.format(DateTimeFormatter.ofPattern(dateFormat));
    }

    public void setEndDate(LocalDate endDate){
        this.endDate = endDate.format(DateTimeFormatter.ofPattern(dateFormat));
    }

    private void addAccounts(int currencyId, int transactionType, String startDate, String endDate){
        String sql = "";
    }

    public void generate(TreeTableView<Report> treeTableView){
        TreeItem<Report> ttvRoot;
        if (treeTableView.getRoot() != null){
            treeTableView.getRoot().getChildren().clear();
            ttvRoot = treeTableView.getRoot();
        }else{
            //  root is not set thus, columns are too
            ttvRoot = new TreeItem<>(new Report("Report", ""));

            TreeTableColumn<Report, String> ttcParameter = new TreeTableColumn<>("Parameter");
            ttcParameter.setPrefWidth(450);
            ttcParameter.setCellValueFactory((TreeTableColumn.CellDataFeatures<Report, String> param) ->
                    new ReadOnlyStringWrapper(param.getValue().getValue().getParameter()));

            TreeTableColumn<Report, String> ttcValue = new TreeTableColumn<>("Value");
            ttcValue.setPrefWidth(150);
            ttcValue.setResizable(false);
            ttcValue.setStyle("-fx-alignment: center-right;");
            ttcValue.setCellValueFactory((TreeTableColumn.CellDataFeatures<Report, String> param) ->
                    new ReadOnlyStringWrapper(param.getValue().getValue().getValue()));

            treeTableView.setRoot(ttvRoot);
            treeTableView.setShowRoot(false);
            treeTableView.getColumns().setAll(ttcParameter, ttcValue);
            ttvRoot.setExpanded(true);
        }

        LocalDate date = LocalDate.parse(startDate, DateTimeFormatter.ofPattern(dateFormat));
        date = date.minusDays(1);
        String dateString = date.format(DateTimeFormatter.ofPattern(dateFormat));
        ttvRoot.getChildren().add(new TreeItem<>(new Report("Total balance on " + startDate + ":", "")));
        addTotalBalanceOnEndOfTheDay(dateString, ttvRoot);
        //  add empty space between rows
        ttvRoot.getChildren().add(new TreeItem<>(new Report("", "")));

        ttvRoot.getChildren().add(new TreeItem<>(new Report("Total balance on " + endDate + ":", "")));
        addTotalBalanceOnEndOfTheDay(endDate, ttvRoot);
        //  add empty space between rows
        ttvRoot.getChildren().add(new TreeItem<>(new Report("", "")));

        String sql = format("select sum(amount_to) as amount, c.name as currency, c.id as currency_id from transactions as t inner join accounts as a on t.account_to_id = a.id inner join currencies as c on a.currencies_id = c.id where t.transaction_types_id = %d and (date between '%s' and '%s') group by currency;", TransactionType.ACCOUNT_TYPES_DEPOSIT, this.startDate, this.endDate);
        List<HashMap<String, String>> rows = Kman.getDB().selectData(sql);
        ttvRoot.getChildren().add(new TreeItem<>(new Report("Total income:", "")));
        for (HashMap<String, String> row: rows){
            ttvRoot.getChildren().add(new TreeItem<>(new Report(format("\t - %s:", row.get("currency")), Strings.userFormat(Float.parseFloat(row.get("amount"))))));

            addRowsToTreeNode(getRowsPerAccount(Integer.parseInt(row.get("currency_id")), true),
                    ttvRoot,
                    "\t\taccounts",
                    "\t\t\t");
            addRowsToTreeNode(getRowsPerPayee(Integer.parseInt(row.get("currency_id")), true),
                    ttvRoot,
                    "\t\tpayees",
                    "\t\t\t");
            addRowsToTreeNode(getRowsPerCategory(Integer.parseInt(row.get("currency_id")), true),
                    ttvRoot,
                    "\t\tcategories",
                    "\t\t\t");
        }

        //  add empty space between rows
        ttvRoot.getChildren().add(new TreeItem<>(new Report("", "")));
        sql = format("select sum(amount_from) as amount, c.name as currency, c.id as currency_id from transactions as t inner join accounts as a on t.account_from_id = a.id inner join currencies as c on a.currencies_id = c.id where t.transaction_types_id = %d and (date between '%s' and '%s') group by currency;", TransactionType.ACCOUNT_TYPES_WITHDRAWAL, this.startDate, this.endDate);
        rows = Kman.getDB().selectData(sql);

        ttvRoot.getChildren().add(new TreeItem<>(new Report("Total expenses:", "")));
        for (HashMap<String, String> row: rows){
            ttvRoot.getChildren().add(new TreeItem<>(new Report(format("\t - %s:", row.get("currency")), Strings.userFormat(Float.parseFloat(row.get("amount"))))));

            addRowsToTreeNode(getRowsPerAccount(Integer.parseInt(row.get("currency_id")), false),
                    ttvRoot,
                    "\t\taccounts",
                    "\t\t\t");
            addRowsToTreeNode(getRowsPerPayee(Integer.parseInt(row.get("currency_id")), false),
                    ttvRoot,
                    "\t\tpayees",
                    "\t\t\t");
            addRowsToTreeNode(getRowsPerCategory(Integer.parseInt(row.get("currency_id")), false),
                    ttvRoot,
                    "\t\tcategories",
                    "\t\t\t");
        }
    }

    /**
     * Returns DB rows per each account
     *
     * @param currencyId take transaction records for the selected currency only
     * @param isDeposit true - return records for deposit, false for withdrawal
     *
     * @return DB records
     */
    private List<HashMap<String, String>> getRowsPerAccount(int currencyId, boolean isDeposit){
        String sql = "";
        if (isDeposit){
            sql = format("select sum(amount_to) as value, a.name as parameter from transactions as t inner join accounts as a on t.account_to_id = a.id where a.currencies_id = %d and t.transaction_types_id = %d and (date between '%s' and '%s') group by parameter order by value desc;", currencyId, TransactionType.ACCOUNT_TYPES_DEPOSIT, this.startDate, this.endDate);
        }else{
            sql = format("select sum(amount_from) as value, a.name as parameter from transactions as t inner join accounts as a on t.account_from_id = a.id where a.currencies_id = %d and t.transaction_types_id = %d and (date between '%s' and '%s') group by parameter order by value desc;", currencyId, TransactionType.ACCOUNT_TYPES_WITHDRAWAL, this.startDate, this.endDate);
        }

//        System.out.println("sql: " + sql);

        return Kman.getDB().selectData(sql);
    }

    /**
     * Returns DB rows per each payee
     *
     * @param currencyId take transaction records for the selected currency only
     * @param isDeposit true - return records for deposit, false for withdrawal
     *
     * @return DB records
     */
    private List<HashMap<String, String>> getRowsPerPayee(int currencyId, boolean isDeposit){
        String sql = "";
        if (isDeposit){
            sql = format("select sum(amount_to) as value, p.name as parameter from transactions as t inner join accounts as a on t.account_to_id = a.id inner join payees as p on p.id = t.payees_id where a.currencies_id = %d and t.transaction_types_id = %d and (date between '%s' and '%s') group by parameter order by value desc;", currencyId, TransactionType.ACCOUNT_TYPES_DEPOSIT, this.startDate, this.endDate);
        }else{
            sql = format("select sum(amount_from) as value, p.name as parameter from transactions as t inner join accounts as a on t.account_from_id = a.id inner join payees as p on p.id = t.payees_id where a.currencies_id = %d and t.transaction_types_id = %d and (date between '%s' and '%s') group by parameter order by value desc;", currencyId, TransactionType.ACCOUNT_TYPES_WITHDRAWAL, this.startDate, this.endDate);
        }

//        System.out.println("sql: " + sql);

        return Kman.getDB().selectData(sql);
    }

    /**
     * Returns DB rows per each category
     *
     * @param currencyId take transaction records for the selected currency only
     * @param isDeposit true - return records for deposit, false for withdrawal
     *
     * @return DB records
     */
    private List<HashMap<String, String>> getRowsPerCategory(int currencyId, boolean isDeposit){
        String sql = "";
        if (isDeposit){
            sql = format("select sum(amount_to) as value, c.name as parameter from transactions as t inner join accounts as a on t.account_to_id = a.id inner join categories as c on c.id = t.categories_id where a.currencies_id = %d and t.transaction_types_id = %d and (date between '%s' and '%s') group by parameter order by value desc;", currencyId, TransactionType.ACCOUNT_TYPES_DEPOSIT, this.startDate, this.endDate);
        }else{
            sql = format("select sum(amount_from) as value, c.name as parameter from transactions as t inner join accounts as a on t.account_from_id = a.id inner join categories as c on c.id = t.categories_id where a.currencies_id = %d and t.transaction_types_id = %d and (date between '%s' and '%s') group by parameter order by value desc;", currencyId, TransactionType.ACCOUNT_TYPES_WITHDRAWAL, this.startDate, this.endDate);
        }

//        System.out.println("sql: " + sql);

        return Kman.getDB().selectData(sql);
    }

    /**
     * Adds rows into TreeItem
     *
     * @param rows rows to be added
     * @param treeNode where to add
     * @param nodeName name of node that will be created
     * @param prefix will be added to text
     */
    private void addRowsToTreeNode(List<HashMap<String, String>> rows, TreeItem<Report> treeNode, String nodeName, String prefix){
        if (!rows.isEmpty()){
            TreeItem<Report> ttvInnerRoot;

            ttvInnerRoot = new TreeItem<>(new Report(nodeName, ""));
            ttvInnerRoot.setExpanded(false);
            for (HashMap<String, String> innerRow: rows){
                ttvInnerRoot.getChildren().add(
                        new TreeItem<>(
                                new Report(prefix + innerRow.get("parameter"), Strings.userFormat(Float.parseFloat(innerRow.get("value"))))
                        )
                );
            }

            treeNode.getChildren().add(ttvInnerRoot);
        }

    }

    /**
     * Adds total balances per each currency for the end of the day of the date
     *
     * @param date
     * @param treeNode
     */
    private void addTotalBalanceOnEndOfTheDay(String date, TreeItem<Report> treeNode){
        //  keeps balance per currency
        Map<String, BigDecimal> currencies = new HashMap<>();
        String currencyName = "";
        BigDecimal balance = null;

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

        for (Map.Entry<String, BigDecimal> currency: currencies.entrySet()){
            treeNode.getChildren().add(
                    new TreeItem<>(
                            new Report(
                                    "\t" + currency.getKey() + ":",
                                    Strings.userFormat(currency.getValue().floatValue())
                            )
                    ));
        }
    }
}