package com.SalesOrderApp.scala

import java.sql.{Connection, DriverManager, ResultSet}
import javax.swing.table.DefaultTableModel
import scala.collection.mutable.ArrayBuffer
import scala.swing._
import scala.swing.event.{SelectionChanged, TableRowsSelected}

object SalesOrderApp extends SimpleSwingApplication {

  // Replace with connection details
  val url = "jdbc:mysql://localhost:3306/northwind"
  val username = "root"
  val password = ""
  val driver = "com.mysql.cj.jdbc.Driver"
  var connection: Connection = _

  // Connecting to Database
  try {
    Class.forName(driver)
    connection = DriverManager.getConnection(url, username, password)
  } catch {
    case e: Exception => e.printStackTrace
  }

  // Get data from database to populate combobox customer ID
  def populateCustomerID: Seq[String] = {
    var data = Seq.empty[String]
    val statement = connection.createStatement()
    val resultSet: ResultSet = statement.executeQuery("SELECT custId FROM customer ORDER BY custId")
    while (resultSet.next()) {
      data :+= resultSet.getString("custId")
    }
    data
  }

  // Get data from database to populate text fields based on selected customer ID
  def populateTextFields(customerId: String, textFields: Seq[TextField]): Unit = {
    val statement = connection.createStatement()
    val resultSet = statement.executeQuery(s"SELECT contactName , companyName, city FROM customer WHERE custId = '$customerId'")
    if (resultSet.next()) {
      textFields(0).text = resultSet.getString("contactName")
      textFields(1).text = resultSet.getString("companyName")
      textFields(2).text = resultSet.getString("city")
    }
  }

  // Get data from database to populate order table based on selected customer ID
  def populateOrderTable(customerId: String): Seq[Array[Any]] = {
    val statement = connection.createStatement()
    val resultSet = statement.executeQuery(s"SELECT so.orderid AS oid, so.orderDate AS od, so.shipCountry AS sco, so.shipcity AS sci FROM salesorder so JOIN customer c ON so.custId = c.custId WHERE c.custId = '$customerId'")
    val tableData = ArrayBuffer[Array[Any]]()
    while (resultSet.next()) {
      val rowData = Array[Any](
        resultSet.getString("oid"),
        resultSet.getDate("od"),
        resultSet.getString("sco"),
        resultSet.getString("sci")
      )
      tableData += rowData
    }
    tableData
  }

  // Get data from database to populate product details table based on selected order ID
  def populateProductDetailsTable(orderId: String): Seq[Array[Any]] = {
    val statement = connection.createStatement()
    val resultSet = statement.executeQuery(s"SELECT p.productId AS pid, od.unitPrice AS up , od.quantity AS q, od.discount AS d FROM salesorder so JOIN orderdetail od ON od.orderId = so.orderId JOIN product p ON p.productId = od.productId WHERE so.orderId =  '$orderId'")
    val tableData = ArrayBuffer[Array[Any]]()
    while (resultSet.next()) {
      val rowData = Array[Any](
        resultSet.getString("pid"),
        resultSet.getDouble("up"),
        resultSet.getInt("q"),
        resultSet.getDouble("d")
      )
      tableData += rowData
    }
    tableData
  }

  // Scala Swing Components Declaration
  def top = new MainFrame {
    title = "Sales Order App"

    // Text fields for Customer Data
    val customertxtFields = List(
      new TextField {columns = 10; enabled = false}, // Customer Name with 10 columns
      new TextField {columns = 15; enabled = false}, // Company Name with 15 columns
      new TextField {columns = 10; enabled = false} // City with 10 columns
    )

    // Text fields for Financial Data
    val valuetxtFields = List(
      new TextField {columns = 10; enabled = false}, // Total Amount with 10 columns
      new TextField {columns = 10; enabled = false}, // Total Discount with 10 columns
      new TextField {columns = 10; enabled = false} // Total Discounted Value  with 10 columns
    )

    // Customer Labels
    val customerLabels = List(
      new Label("Customer Name: "),
      new Label("Company Name: "),
      new Label("City: "),
      new Label("  "),
      new Label("Customer ID: ")
    )

    // Value Labels
    val valueLabels = List(
      new Label("There are products for Order : ")
    )

    // Combo box for customer ID
    val cmbCustomerId = new ComboBox(populateCustomerID)

    // Table model for order table
    val tableModel = new DefaultTableModel(0, 4) {
      override def getColumnName(column: Int): String = {
        column match {
          case 0 => "Order Id"
          case 1 => "Order Date"
          case 2 => "Ship Country"
          case 3 => "Ship City"
          case _ => ""
        }
      }
      override def isCellEditable(row: Int, column: Int): Boolean = false
    }

    // Table for displaying order data
    val orderTable = new Table {
      model = tableModel
    }

    // Table model for product details table
    val productDetailsTableModel = new DefaultTableModel(0,4 ) {
      override def getColumnName(column: Int): String = {
        column match {
          case 0 => "Product Id"
          case 1 => "Unit Price"
          case 2 => "Quantity"
          case 3 => "Percent Discount"
          case _ => ""
        }
      }
      override def isCellEditable(row: Int, column: Int): Boolean = false
    }

    // Table for displaying product details data
    val productDetailsTable = new Table {
      model = productDetailsTableModel
    }

    // Create a scroll pane for the table
    val orderScrollPane = new ScrollPane(orderTable)
    val prodDeetScrollPane = new ScrollPane(productDetailsTable)

    // Event listener for combo box selection change
    listenTo(cmbCustomerId.selection)
    reactions += {
      case SelectionChanged(_) =>
        val selectedCustomerId = cmbCustomerId.selection.item
        populateTextFields(selectedCustomerId, customertxtFields)

        val orderData = populateOrderTable(selectedCustomerId)
        tableModel.setRowCount(0) // Clear previous data
        orderData.foreach(row => tableModel.addRow(row.asInstanceOf[Array[Object]]))

    }

    // Event listener for row selection change in order table
    listenTo(orderTable.selection)
    reactions += {
      case TableRowsSelected(_, _, false) =>
        val selectedRow = orderTable.selection.rows.headOption.getOrElse(-1)
        if (selectedRow != -1) {
          val orderId = orderTable.model.getValueAt(selectedRow, 0).toString
          val productDetailsData = populateProductDetailsTable(orderId)
          productDetailsTableModel.setRowCount(0) // Clear previous data
          productDetailsData.foreach(row => productDetailsTableModel.addRow(row.asInstanceOf[Array[Object]]))
        }
    }

    // Border Panel
    contents = new BorderPanel {
      layout(new BoxPanel(Orientation.Vertical) {
        contents ++= customerLabels
        border = Swing.EmptyBorder(30, 30, 10, 30)
      }) = BorderPanel.Position.West

      layout(new BoxPanel(Orientation.Vertical) {
        contents ++= customertxtFields
        contents += cmbCustomerId
        border = Swing.EmptyBorder(30, 30, 10, 30)
      }) = BorderPanel.Position.Center

      layout(new BoxPanel(Orientation.Vertical) {
        contents += prodDeetScrollPane
        border = Swing.EmptyBorder(30, 30, 10, 30)
      }) = BorderPanel.Position.East

      layout(new BoxPanel(Orientation.Vertical) {
        contents += orderScrollPane
        contents ++= valuetxtFields
        border = Swing.EmptyBorder(30, 30, 30, 30)
      }) = BorderPanel.Position.South

    }

  }
}

