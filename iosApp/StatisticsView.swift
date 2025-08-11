

//
//  StatisticsView.swift
//
//
//  Created by mymac on 08/08/2025.
//
import SwiftUI
import Charts
import FirebaseAuth
import FirebaseFirestore

// MARK: - Main Statistics View
struct StatisticsView: View {
    @StateObject private var statsManager = StatisticsManager()
    
    private let statColumns = [
        GridItem(.flexible()),
        GridItem(.flexible())
    ]
    
    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(spacing: 20) {
                    // Header with logo
                    HStack {
                        Text("🐾")
                            .font(.system(size: 32))
                        Text("PetPals Statistics")
                            .font(.title2)
                            .fontWeight(.bold)
                            .foregroundColor(.logoBrown)
                    }
                    
                    if statsManager.isLoading {
                        ProgressView("טוען נתונים...")
                            .frame(height: 200)
                    } else {
                        // Quick stat cards
                        LazyVGrid(columns: statColumns, spacing: 12) {
                            QuickStatCard(card: StatCard(title: "פוסטים", value: "\(statsManager.totalPosts)", icon: "camera.fill", color: .pink))
                            QuickStatCard(card: StatCard(title: "לייקים", value: "\(statsManager.totalLikes)", icon: "heart.fill", color: .red))
                            QuickStatCard(card: StatCard(title: "תגובות", value: "\(statsManager.totalComments)", icon: "message.fill", color: .blue))
                            QuickStatCard(card: StatCard(title: "מרחק", value: String(format: "%.1f ק\"מ", statsManager.totalDistance), icon: "location.fill", color: .green))
                        }
                        
                        // Advanced stats row
                        LazyVGrid(columns: statColumns, spacing: 12) {
                            QuickStatCard(card: StatCard(title: "ימים פעילים החודש", value: "\(statsManager.activeDaysThisMonth)", icon: "calendar.badge.clock", color: .purple))
                            QuickStatCard(card: StatCard(title: "רצף ימים", value: "\(statsManager.streakDays)", icon: "flame.fill", color: .orange))
                        }
                        
                        // Additional insights
                        InsightsCardView(statsManager: statsManager)
                        
                        // Monthly activity chart
                        if !statsManager.monthlyStats.isEmpty {
                            MonthlyChartView(monthlyStats: statsManager.monthlyStats)
                        }
                        
                        // Monthly breakdown
                        MonthlyBreakdownView(monthlyStats: statsManager.monthlyStats)
                        
                        // Achievements
                        AchievementsView(achievements: statsManager.getAchievements())
                    }
                }
                .padding()
            }
            .navigationTitle("סטטיסטיקות")
            .refreshable {
                await statsManager.refreshStats()
            }
        }
        .onAppear {
            statsManager.loadStatistics()
        }
    }
}

// MARK: - Insights Card
struct InsightsCardView: View {
    let statsManager: StatisticsManager
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("תובנות מעניינות")
                .font(.title3)
                .fontWeight(.bold)
            
            VStack(spacing: 8) {
                InsightRow(
                    icon: "chart.bar.fill",
                    title: "ממוצע לייקים לפוסט",
                    value: String(format: "%.1f", statsManager.averageLikesPerPost),
                    color: .purple
                )
                
                InsightRow(
                    icon: "calendar.badge.plus",
                    title: "היום הכי פעיל",
                    value: statsManager.mostActiveDay,
                    color: .blue
                )
                
                InsightRow(
                    icon: "mappin.and.ellipse",
                    title: "מקום מועדף",
                    value: statsManager.favoriteLocation,
                    color: .green
                )
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(radius: 2)
    }
}

struct InsightRow: View {
    let icon: String
    let title: String
    let value: String
    let color: Color
    
    var body: some View {
        HStack {
            Image(systemName: icon)
                .foregroundColor(color)
                .frame(width: 20)
            
            Text(title)
                .font(.subheadline)
                .foregroundColor(.secondary)
            
            Spacer()
            
            Text(value)
                .font(.subheadline)
                .fontWeight(.medium)
                .foregroundColor(color)
        }
    }
}

// MARK: - Monthly Chart View
struct MonthlyChartView: View {
    let monthlyStats: [MonthlyStats]
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("פעילות חודשית")
                .font(.title3)
                .fontWeight(.bold)
            
            Chart(monthlyStats) { stat in
                // Posts line
                LineMark(
                    x: .value("חודש", stat.month),
                    y: .value("פוסטים", stat.postsCount)
                )
                .foregroundStyle(.blue)
                .lineStyle(StrokeStyle(lineWidth: 3))
                .interpolationMethod(.catmullRom)
                .symbol(.circle)
                
                // Likes line
                LineMark(
                    x: .value("חודש", stat.month),
                    y: .value("לייקים", stat.likesReceived)
                )
                .foregroundStyle(.red)
                .lineStyle(StrokeStyle(lineWidth: 2, dash: [5, 3]))
                .symbol(.triangle)
            }
            .frame(height: 200)
            .chartYAxis {
                AxisMarks(position: .leading)
            }
            .chartXAxis {
                AxisMarks(values: .automatic) { value in
                    AxisGridLine()
                    AxisValueLabel() {
                        if let month = value.as(String.self) {
                            Text(month)
                                .font(.caption2)
                                .rotationEffect(.degrees(-45))
                        }
                    }
                }
            }
            .chartLegend(position: .bottom, alignment: .center) {
                HStack(spacing: 20) {
                    HStack(spacing: 4) {
                        Circle()
                            .fill(.blue)
                            .frame(width: 8, height: 8)
                        Text("פוסטים")
                            .font(.caption)
                    }
                    
                    HStack(spacing: 4) {
                        Circle()
                            .fill(.red)
                            .frame(width: 8, height: 8)
                        Text("לייקים")
                            .font(.caption)
                    }
                }
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(radius: 2)
    }
}

// MARK: - Monthly Breakdown View
struct MonthlyBreakdownView: View {
    let monthlyStats: [MonthlyStats]
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("פירוט חודשי")
                .font(.title3)
                .fontWeight(.bold)
            
            ForEach(monthlyStats.reversed()) { stat in
                MonthlyStatCard(stat: stat)
            }
        }
    }
}

// MARK: - Monthly Stat Card (Updated)
struct MonthlyStatCard: View {
    let stat: MonthlyStats
    
    var body: some View {
        VStack(spacing: 12) {
            HStack {
                Text(stat.month)
                    .font(.headline)
                    .fontWeight(.bold)
                
                Spacer()
                
                Text("🐾")
                    .font(.title3)
            }
            
            LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 2), spacing: 16) {
                StatItem(title: "פוסטים", value: "\(stat.postsCount)", color: .pink)
                StatItem(title: "לייקים", value: "\(stat.likesReceived)", color: .red)
                StatItem(title: "תגובות", value: "\(stat.commentsCount)", color: .blue)
                StatItem(title: "מרחק", value: String(format: "%.1fק\"מ", stat.totalDistance), color: .green)
            }
            
            // Progress bar for active days
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text("ימים פעילים")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Spacer()
                    Text("\(stat.activeDays)/31")
                        .font(.caption)
                        .fontWeight(.medium)
                        .foregroundColor(.purple)
                }
                
                ProgressView(value: Double(stat.activeDays), total: 31.0)
                    .progressViewStyle(.linear)
                    .tint(.purple)
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(16)
        .shadow(radius: 2)
    }
}

// MARK: - Achievements View
struct AchievementsView: View {
    let achievements: [Achievement]
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Text("🏆")
                    .font(.title2)
                Text("הישגים")
                    .font(.title3)
                    .fontWeight(.bold)
            }
            
            LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 2), spacing: 12) {
                ForEach(achievements) { achievement in
                    AchievementCard(achievement: achievement)
                }
            }
        }
        .padding()
        .background(Color.logoBrown.opacity(0.05))
        .cornerRadius(16)
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(Color.logoBrown.opacity(0.2), lineWidth: 1)
        )
    }
}

struct AchievementCard: View {
    let achievement: Achievement
    
    var body: some View {
        VStack(spacing: 8) {
            Text(achievement.icon)
                .font(.system(size: 24))
            
            Text(achievement.title)
                .font(.subheadline)
                .fontWeight(.bold)
                .multilineTextAlignment(.center)
                .foregroundColor(achievement.color.swiftUIColor)
            
            Text(achievement.description)
                .font(.caption2)
                .multilineTextAlignment(.center)
                .foregroundColor(.secondary)
                .lineLimit(2)
        }
        .frame(height: 100)
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 8)
        .background(achievement.color.swiftUIColor.opacity(0.1))
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(achievement.color.swiftUIColor.opacity(0.3), lineWidth: 1)
        )
    }
}

// MARK: - Quick Stat Card (Updated)
struct QuickStatCard: View {
    let card: StatCard
    
    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: card.icon)
                .font(.title2)
                .foregroundColor(card.color)
            
            Text(card.value)
                .font(.title3)
                .fontWeight(.bold)
                .foregroundColor(card.color)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
            
            Text(card.title)
                .font(.caption)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .lineLimit(2)
        }
        .frame(height: 100)
        .frame(maxWidth: .infinity)
        .background(card.color.opacity(0.1))
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(card.color.opacity(0.3), lineWidth: 1)
        )
    }
}

// MARK: - Stat Item (Updated)
struct StatItem: View {
    let title: String
    let value: String
    let color: Color
    
    var body: some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.subheadline)
                .fontWeight(.bold)
                .foregroundColor(color)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
            
            Text(title)
                .font(.caption2)
                .foregroundColor(.secondary)
                .lineLimit(1)
        }
    }
}

// MARK: - StatCard Model
struct StatCard {
    let title: String
    let value: String
    let icon: String
    let color: Color
}
