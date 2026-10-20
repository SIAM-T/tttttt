package com.example.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Sale
import com.example.ui.components.SizeBadge
import com.example.ui.theme.BkashPink
import com.example.ui.theme.BkashPinkContainer
import com.example.ui.theme.DokanGreen
import com.example.ui.theme.DokanGreenContainer
import com.example.ui.theme.DokanGreenDark
import com.example.ui.theme.DokanGreenLight
import com.example.ui.theme.DueAmber
import com.example.ui.theme.DueAmberContainer
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LightBorder
import com.example.ui.theme.NagadOrange
import com.example.ui.theme.NagadOrangeContainer
import com.example.util.StorageHelper
import com.example.viewmodel.DetailedReportData
import com.example.viewmodel.ReportTopProduct
import com.example.viewmodel.RetailViewModel
import com.example.viewmodel.TimeframeFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailReportScreen(
    viewModel: RetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val reportData by viewModel.detailedReport.collectAsStateWithLifecycle()
    val currentTimeframe by viewModel.reportTimeframe.collectAsStateWithLifecycle()

    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "দোকানের বিস্তারিত হিসাব ও রিপোর্ট",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Sales Analytics, Due & Payment Ledger",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("report_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val shareIntent = viewModel.shareDetailedReportText(context, reportData)
                            context.startActivity(shareIntent)
                        },
                        modifier = Modifier.testTag("report_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Report",
                            tint = DokanGreen
                        )
                    }
                    IconButton(
                        onClick = {
                            val csvIntent = viewModel.exportSalesCsv(context)
                            csvIntent?.let { context.startActivity(it) }
                        },
                        modifier = Modifier.testTag("report_csv_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Export CSV",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Timeframe Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimeframeFilter.entries.forEach { filter ->
                        val isSelected = currentTimeframe == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setReportTimeframe(filter) },
                            label = {
                                Text(
                                    text = filter.banglaLabel,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DokanGreen,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }

            // Hero Financial Performance Card
            item {
                HeroReportCard(reportData = reportData)
            }

            // Profit & Loss Performance Card
            item {
                ProfitAnalysisCard(reportData = reportData)
            }

            // Key Metrics 2x2 Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricMiniCard(
                        title = "নগদ আদায়",
                        value = StorageHelper.formatCurrency(reportData.totalCashCollected),
                        subtitle = "নগদ + মোবাইল ব্যাংকিং",
                        accentColor = DokanGreen,
                        icon = Icons.Default.Paid,
                        modifier = Modifier.weight(1f)
                    )
                    MetricMiniCard(
                        title = "বাকি বিক্রি",
                        value = StorageHelper.formatCurrency(reportData.totalDueGiven),
                        subtitle = "খাতায় পাওনা",
                        accentColor = DueAmber,
                        icon = Icons.Default.Warning,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricMiniCard(
                        title = "বিক্রিত আইটেম",
                        value = "${reportData.totalItemsSold} টি",
                        subtitle = "${reportData.totalInvoices} টি মেমো",
                        accentColor = Color(0xFF2563EB),
                        icon = Icons.Default.ShoppingBag,
                        modifier = Modifier.weight(1f)
                    )
                    MetricMiniCard(
                        title = "গড় বিক্রি / মেমো",
                        value = StorageHelper.formatCurrency(reportData.averageOrderValue),
                        subtitle = "Average Order Value",
                        accentColor = Color(0xFF7C3AED),
                        icon = Icons.Default.Assessment,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Payment Channel Breakdown Card
            item {
                PaymentBreakdownCard(reportData = reportData)
            }

            // Top Selling Products Section
            if (reportData.topProducts.isNotEmpty()) {
                item {
                    Text(
                        text = "🏆 শীর্ষ বিক্রিত পণ্য (${reportData.timeframe.banglaLabel})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                items(reportData.topProducts) { prod ->
                    TopProductItem(prod = prod)
                }
            }

            // Itemized Transaction Ledger Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📋 বিস্তারিত মেমো তালিকা (${reportData.salesList.size} টি)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (reportData.salesList.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "এই সময়কালে কোনো বিক্রির রেকর্ড পাওয়া যায়নি",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(reportData.salesList) { sale ->
                    ReportSaleRow(sale = sale)
                }
            }

            // Bottom Share Action
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val shareIntent = viewModel.shareDetailedReportText(context, reportData)
                        context.startActivity(shareIntent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DokanGreen),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "হোয়াটসঅ্যাপে রিপোর্ট পাঠান (Share Statement)",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroReportCard(reportData: DetailedReportData) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DokanGreen.copy(alpha = 0.2f), RoundedCornerShape(22.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            DokanGreenContainer.copy(alpha = 0.7f),
                            Color.White
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "মোট বিক্রয় আয় (${reportData.timeframe.banglaLabel})",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = DokanGreenDark
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DokanGreen.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${reportData.totalInvoices} টি রসিদ",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = DokanGreenDark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = StorageHelper.formatCurrency(reportData.totalRevenue),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Highlight Profit directly in Hero Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DokanGreen.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = DokanGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "মোট অর্জিত লাভ (Gross Profit):",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = DokanGreenDark
                            )
                        }
                        Text(
                            text = if (reportData.grossProfit >= 0) "+${StorageHelper.formatCurrency(reportData.grossProfit)} (${String.format(java.util.Locale.US, "%.1f", reportData.profitMarginPercent)}%)" else "-${StorageHelper.formatCurrency(-reportData.grossProfit)}",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = if (reportData.grossProfit >= 0) DokanGreen else ErrorRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val collectionRate = if (reportData.totalRevenue > 0) {
                    ((reportData.totalCashCollected / reportData.totalRevenue) * 100).toInt()
                } else 100

                Text(
                    text = "নগদ আদায়ের হার: $collectionRate% • বাকি দেওয়া হয়েছে: ${StorageHelper.formatCurrency(reportData.totalDueGiven)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = if (reportData.totalDueGiven > 0) DueAmber else DokanGreenDark
                )
            }
        }
    }
}

@Composable
private fun ProfitAnalysisCard(reportData: DetailedReportData) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DokanGreenLight.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(DokanGreenContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = DokanGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "মুনাফা ও লাভ-ক্ষতির বিশ্লেষণ",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "বিক্রি আয় বনাম পণ্য কেনার খরচের হিসাব",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (reportData.grossProfit >= 0) DokanGreenContainer else Color(0xFFFEE2E2)
                ) {
                    Text(
                        text = if (reportData.grossProfit >= 0) "${String.format(java.util.Locale.US, "%.1f", reportData.profitMarginPercent)}% মার্জিন" else "লোকসান",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (reportData.grossProfit >= 0) DokanGreenDark else ErrorRed,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Profit Stat Box
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "মোট আনুমানিক লাভ (Gross Profit)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (reportData.grossProfit >= 0) "+${StorageHelper.formatCurrency(reportData.grossProfit)}" else "-${StorageHelper.formatCurrency(-reportData.grossProfit)}",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = if (reportData.grossProfit >= 0) DokanGreen else ErrorRed
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (reportData.grossProfit >= 0) DokanGreen.copy(alpha = 0.12f) else ErrorRed.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = if (reportData.grossProfit >= 0) "লাভজনক" else "সতর্কতা",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (reportData.grossProfit >= 0) DokanGreenDark else ErrorRed,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2 Column Breakdown: Cost of Goods Sold vs Realized Cash Profit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Cost of goods sold
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "মোট কেনা খরচ (COGS)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = StorageHelper.formatCurrency(reportData.totalCost),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Realized Cash Profit
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "নগদ আদায়কৃত লাভ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = StorageHelper.formatCurrency(reportData.realizedCashProfit),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (reportData.realizedCashProfit >= 0) DokanGreenDark else DueAmber
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricMiniCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PaymentBreakdownCard(reportData: DetailedReportData) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "💳 পেমেন্ট পদ্ধতি বিশ্লেষণ",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            val total = reportData.totalRevenue.coerceAtLeast(1.0)

            PaymentChannelRow(
                name = "নগদ ক্যাশ (Cash)",
                amount = reportData.cashSalesAmount,
                ratio = (reportData.cashSalesAmount / total).toFloat(),
                barColor = DokanGreen
            )

            Spacer(modifier = Modifier.height(10.dp))

            PaymentChannelRow(
                name = "বিকাশ (bKash)",
                amount = reportData.bkashSalesAmount,
                ratio = (reportData.bkashSalesAmount / total).toFloat(),
                barColor = BkashPink
            )

            Spacer(modifier = Modifier.height(10.dp))

            PaymentChannelRow(
                name = "নগদ মোবাইল (Nagad)",
                amount = reportData.nagadSalesAmount,
                ratio = (reportData.nagadSalesAmount / total).toFloat(),
                barColor = NagadOrange
            )

            if (reportData.totalDueGiven > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                PaymentChannelRow(
                    name = "বাকি পাওনা (Credit / Due)",
                    amount = reportData.totalDueGiven,
                    ratio = (reportData.totalDueGiven / total).toFloat(),
                    barColor = DueAmber
                )
            }
        }
    }
}

@Composable
private fun PaymentChannelRow(
    name: String,
    amount: Double,
    ratio: Float,
    barColor: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${StorageHelper.formatCurrency(amount)} (${(ratio * 100).toInt()}%)",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = barColor
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { ratio.coerceIn(0f, 1f) },
            color = barColor,
            trackColor = barColor.copy(alpha = 0.15f),
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )
    }
}

@Composable
private fun TopProductItem(prod: ReportTopProduct) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = prod.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (prod.size.isNotBlank() && prod.size != "Standard") {
                        Spacer(modifier = Modifier.width(6.dp))
                        SizeBadge(size = prod.size)
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "মোট বিক্রি: ${prod.quantitySold} টি ইউনিট",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = StorageHelper.formatCurrency(prod.totalRevenue),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = DokanGreen
                )
                if (prod.estimatedProfit > 0) {
                    Text(
                        text = "লাভ: +${StorageHelper.formatCurrency(prod.estimatedProfit)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = DokanGreenDark
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportSaleRow(sale: Sale) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sale.productName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${sale.customerName} • ${StorageHelper.formatDate(sale.saleTimestamp)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = StorageHelper.formatCurrency(sale.totalAmount),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${sale.quantity} x ${StorageHelper.formatCurrency(sale.unitPrice)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Payment Method Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = sale.paymentMethod,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Due or Paid Badge
                if (sale.dueAmount > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DueAmber.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "বাকি: ${StorageHelper.formatCurrency(sale.dueAmount)} (আদায়: ${StorageHelper.formatCurrency(sale.paidAmount)})",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = DueAmber,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DokanGreen.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "পরিশোধিত (Paid)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = DokanGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
