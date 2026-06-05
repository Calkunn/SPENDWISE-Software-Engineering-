package com.example.tugasmobile

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    // Menyimpan semua transaksi
    private lateinit var transactions: List<Transaction>

    // Adapter RecyclerView
    private lateinit var transactionAdapter: TransactionAdapter

    // Layout manager RecyclerView
    private lateinit var linearLayoutManager: LinearLayoutManager

    // Database Room
    private lateinit var db: AppDatabase

    // Menyimpan transaksi yang dihapus sementara
    private lateinit var deletedTransaction: Transaction

    // Menyimpan semua transaksi lama untuk fitur undo
    private lateinit var oldTransactions: List<Transaction>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Menghubungkan activity dengan layout XML
        setContentView(R.layout.activity_main)

        // Inisialisasi list transaksi kosong
        transactions = arrayListOf()

        // Membuat adapter RecyclerView
        transactionAdapter = TransactionAdapter(transactions)

        // Mengatur RecyclerView vertical
        linearLayoutManager = LinearLayoutManager(this)

        // Membuat database Room
        db = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "transactions"
        ).build()

        // Mengambil RecyclerView dari XML
        val recycleView = findViewById<RecyclerView>(R.id.recycle_view)

        // Mengambil tombol tambah
        val addBtn = findViewById<FloatingActionButton>(R.id.addBtn)

        // Menghubungkan adapter ke RecyclerView
        recycleView.adapter = transactionAdapter

        // Menghubungkan layout manager ke RecyclerView
        recycleView.layoutManager = linearLayoutManager

        // Membuat swipe helper
        val itemTouchHelper = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.RIGHT
        ) {

            // Tidak menggunakan drag and drop
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            // Dijalankan saat item di swipe
            override fun onSwiped(
                viewHolder: RecyclerView.ViewHolder,
                direction: Int
            ) {

                // Mengambil posisi item yang di swipe
                val position = viewHolder.adapterPosition

                // Menghapus transaksi sesuai posisi
                deleteTransaction(transactions[position])
            }
        }

        // Menghubungkan swipe helper ke RecyclerView
        val swipeHelper = ItemTouchHelper(itemTouchHelper)
        swipeHelper.attachToRecyclerView(recycleView)

        // Event tombol tambah
        addBtn.setOnClickListener {

            // Pindah ke AddTransactionActivity
            val intent = Intent(this, AddTransactionActivity::class.java)
            startActivity(intent)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)

    // Mengambil semua transaksi dari database
    private fun fetchAll() {

        GlobalScope.launch {

            // Mengambil semua data transaksi
            transactions = db.transactionDao().getAll()

            runOnUiThread {

                // Update dashboard
                updateDashboard()

                // Refresh RecyclerView
                transactionAdapter.setData(transactions)
            }
        }
    }

    // Mengupdate total balance, budget, dan expenses
    private fun updateDashboard() {

        val balance = findViewById<TextView>(R.id.Balance)
        val budget = findViewById<TextView>(R.id.budget)
        val expense = findViewById<TextView>(R.id.expenses)

        // Menghitung total saldo
        val totalAmount = transactions.map { it.amount }.sum()

        // Menghitung total pemasukan
        val budgetAmount = transactions
            .filter { it.amount > 0 }
            .map { it.amount }
            .sum()

        // Menghitung total pengeluaran
        val expenseAmount = totalAmount - budgetAmount

        // Menampilkan total balance
        balance.text = "Rp.%.0f".format(totalAmount)

        // Menampilkan budget
        budget.text = "Rp.%.0f".format(budgetAmount)

        // Menampilkan expenses
        expense.text = "Rp.%.0f".format(expenseAmount)
    }

    @OptIn(DelicateCoroutinesApi::class)

    // Function untuk menghapus transaksi
    private fun deleteTransaction(transaction: Transaction) {

        // Menyimpan transaksi yang dihapus
        deletedTransaction = transaction

        // Menyimpan semua transaksi lama
        oldTransactions = transactions

        GlobalScope.launch {

            // Menghapus transaksi dari database
            db.transactionDao().delete(
                listOf(transaction)
            )

            // Mengupdate list transaksi
            transactions = transactions.filter {
                it.id != transaction.id
            }

            runOnUiThread {

                // Update dashboard
                updateDashboard()

                // Refresh RecyclerView
                transactionAdapter.setData(transactions)

                // Menampilkan snackbar
                showSnackBar()
            }
        }
    }

    // Menampilkan snackbar setelah transaksi dihapus
    private fun showSnackBar() {

        // Mengambil root layout
        val view = findViewById<android.view.View>(R.id.main)

        // Membuat snackbar
        val snackbar = Snackbar.make(
            view,
            "Transaction Deleted",
            Snackbar.LENGTH_LONG
        )

        // Tombol undo
        snackbar.setAction("Undo") {

            // Menjalankan undo delete
            undoDelete()
        }

        // Mengubah warna tombol undo menjadi merah
        snackbar.setActionTextColor(
            ContextCompat.getColor(this, R.color.red)
        )

        // Menampilkan snackbar
        snackbar.show()
    }

    @OptIn(DelicateCoroutinesApi::class)

    // Mengembalikan transaksi yang dihapus
    private fun undoDelete() {

        GlobalScope.launch {

            // Insert kembali transaksi yang dihapus
            db.transactionDao().insertAll(
                listOf(deletedTransaction)
            )

            // Mengembalikan list transaksi lama
            transactions = oldTransactions

            runOnUiThread {

                // Update dashboard
                updateDashboard()

                // Refresh RecyclerView
                transactionAdapter.setData(transactions)
            }
        }
    }

    // Dijalankan saat activity kembali aktif
    override fun onResume() {
        super.onResume()

        // Mengambil ulang data transaksi
        fetchAll()
    }
}