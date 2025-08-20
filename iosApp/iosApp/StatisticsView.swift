
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
                    // Header with logo + title
                    HStack {
                        Text("🐾")
                            .font(.system(size: 32))
                        Text("stats.header")
                            .font(.title2)
                            .fontWeight(.bold)
                            .foregroundColor(.logoBrown)
                    }
                    
                    if statsManager.isLoading {
                        VStack(spacing: 12) {
                            ProgressView()
                            Text("stats.loading")
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                        }
                        .frame(height: 200)
                    } else {
                        // Quick stat cards
                        LazyVGrid(columns: statColumns, spacing: 12) {
                            QuickStatCard(card: StatCard(title: "stats.quick.posts",
                                                         value: "\(statsManager.totalPosts)",
                                                         icon: "camera.fill",
                                                         color: .pink))
                            QuickStatCard(card: StatCard(title: "stats.quick.likes",
                                                         value: "\(statsManager.totalLikes)",
                                                         icon: "heart.fill",
                                                         color: .red))
                            QuickStatCard(card: StatCard(title: "stats.quick.comments",
                                                         value: "\(statsManager.totalComments)",
                                                         icon: "message.fill",
                                                         color: .blue))
                            QuickStatCard(card: StatCard(title: "stats.breakdown.distance",
                                                         value: "\(String(format: "%.1f", statsManager.totalDistance)) \(String(localized: "unit.km"))",
                                                         icon: "location.fill",
                                                         color: .green))
                        }
                        
                        // Advanced stats row
                        LazyVGrid(columns: statColumns, spacing: 12) {
                            QuickStatCard(card: StatCard(title: "stats.quick.active_days",
                                                         value: "\(statsManager.activeDaysThisMonth)",
                                                         icon: "calendar.badge.clock",
                                                         color: .purple))
                            QuickStatCard(card: StatCard(title: "stats.quick.streak",
                                                         value: "\(statsManager.streakDays)",
                                                         icon: "flame.fill",
                                                         color: .orange))
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
            .navigationTitle(Text("stats.title"))
            .refreshable {
                await statsManager.refreshStats()
            }
        }
        .onAppear { statsManager.loadStatistics() }
    }
}

// MARK: - Insights Card
struct InsightsCardView: View {
    let statsManager: StatisticsManager
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("stats.insights.title")
                .font(.title3)
                .fontWeight(.bold)
            
            VStack(spacing: 8) {
                InsightRow(
                    icon: "chart.bar.fill",
                    title: "stats.insights.avg_likes",
                    value: String(format: "%.1f", statsManager.averageLikesPerPost),
                    color: .purple
                )
                InsightRow(
                    icon: "calendar.badge.plus",
                    title: "stats.insights.most_active_day",
                    value: statsManager.mostActiveDay,
                    color: .blue
                )
                InsightRow(
                    icon: "mappin.and.ellipse",
                    title: "stats.insights.favorite_location",
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
    let title: LocalizedStringKey   // ← היה String
    let value: String
    let color: Color
    
    var body: some View {
        HStack {
            Image(systemName: icon)
                .foregroundColor(color)
                .frame(width: 20)
            
            Text(title)              // ← עכשיו מקבל מפתח
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
            Text("stats.monthly.title")
                .font(.title3)
                .fontWeight(.bold)
            
            Chart(monthlyStats) { stat in
                LineMark(
                    x: .value("month", stat.month),
                    y: .value(String(localized: "stats.monthly.legend.posts"), stat.postsCount)
                )
                .foregroundStyle(.blue)
                .lineStyle(StrokeStyle(lineWidth: 3))
                .interpolationMethod(.catmullRom)
                .symbol(.circle)
                
                LineMark(
                    x: .value("month", stat.month),
                    y: .value(String(localized: "stats.monthly.legend.likes"), stat.likesReceived)
                )
                .foregroundStyle(.red)
                .lineStyle(StrokeStyle(lineWidth: 2, dash: [5, 3]))
                .symbol(.triangle)
            }
            .frame(height: 200)
            .chartYAxis { AxisMarks(position: .leading) }
            .chartXAxis { AxisMarks(values: .automatic) }
            .chartLegend(position: .bottom, alignment: .center) {
                HStack(spacing: 20) {
                    HStack(spacing: 4) {
                        Circle().fill(.blue).frame(width: 8, height: 8)
                        Text("stats.monthly.legend.posts").font(.caption)
                    }
                    HStack(spacing: 4) {
                        Circle().fill(.red).frame(width: 8, height: 8)
                        Text("stats.monthly.legend.likes").font(.caption)
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
            Text("stats.breakdown.title")
                .font(.title3)
                .fontWeight(.bold)
            
            ForEach(monthlyStats.reversed()) { stat in
                MonthlyStatCard(stat: stat)
            }
        }
    }
}

// MARK: - Monthly Stat Card
struct MonthlyStatCard: View {
    let stat: MonthlyStats
    
    var body: some View {
        VStack(spacing: 12) {
            HStack {
                Text(stat.month)
                    .font(.headline)
                    .fontWeight(.bold)
                Spacer()
                Text("🐾").font(.title3)
            }
            
            LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 2), spacing: 16) {
                StatItem(title: "stats.quick.posts",
                         value: "\(stat.postsCount)",
                         color: .pink)
                StatItem(title: "stats.quick.likes",
                         value: "\(stat.likesReceived)",
                         color: .red)
                StatItem(title: "stats.quick.comments",
                         value: "\(stat.commentsCount)",
                         color: .blue)
                StatItem(title: "stats.breakdown.distance",
                         value: "\(String(format: "%.1f", stat.totalDistance)) \(String(localized: "unit.km"))",
                         color: .green)
            }
            
            // Progress bar for active days
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text("stats.breakdown.active_days")
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

// MARK: - Achievements View (הטקסטים כאן מגיעים מהמודל עצמו)
struct AchievementsView: View {
    let achievements: [Achievement]
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Text("🏆").font(.title2)
                Text("stats.achievements.title")
                    .font(.title3).fontWeight(.bold)
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

            // היה: Text(achievement.title)
            Text(LocalizedStringKey(achievement.title))
                .font(.subheadline)
                .fontWeight(.bold)
                .multilineTextAlignment(.center)
                .foregroundColor(achievement.color.swiftUIColor)

            // היה: Text(achievement.description)
            Text(LocalizedStringKey(achievement.description))
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



// MARK: - Quick Stat Card
struct QuickStatCard: View {
    let card: StatCard  // title: LocalizedStringKey
    
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
            Text(card.title)             // ← LocalizedStringKey, יחליף שפה בזמן ריצה
                .font(.caption)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .lineLimit(2)
        }
        .frame(height: 100)
        .frame(maxWidth: .infinity)
        .background(card.color.opacity(0.1))
        .cornerRadius(12)
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(card.color.opacity(0.3), lineWidth: 1))
    }
}

// MARK: - Stat Item
struct StatItem: View {
    let title: LocalizedStringKey   // ← היה String
    let value: String
    let color: Color
    
    var body: some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.subheadline).fontWeight(.bold)
                .foregroundColor(color)
                .lineLimit(1).minimumScaleFactor(0.8)
            Text(title)                 // ← מפתח
                .font(.caption2).foregroundColor(.secondary)
                .lineLimit(1)
        }
    }
}

// MARK: - StatCard Model
struct StatCard {
    let title: LocalizedStringKey   // ← היה String
    let value: String
    let icon: String
    let color: Color
}
