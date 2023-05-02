package edu.depauw.csc480.projectv1;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.Scanner;

import org.apache.derby.jdbc.EmbeddedDriver;

/**
 * 
 * 
 * @author Minh Do
 */
public class Main {
	private static final Scanner in = new Scanner(System.in);
	private static final PrintStream out = System.out;

	public static void main(String[] args) {
		try (Connection conn = getConnection("jdbc:derby:db/studentdb")) {
			displayMenu();
			loop: while (true) {
				switch (requestString("Selection (0 to quit, 9 for menu)? ")) {
				case "0": // Quit
					break loop;

				case "1": // Reset
					resetTables(conn);
					break;

				case "2": // list Players
					listPlayers(conn);
					break;

				case "3": // Display League information (given the league name)
					LeagueInfomation(conn);
					break;
				case "4": // Team Line up
				    listPlayersinTeam(conn);	
                    break;
				case "5": // Update Line-up - Add a new player to an existing team
					updateLineUp(conn);
					break;

				case "6": // Change Market Value of a player
					changeMarketvalue(conn);
					break;

				case "7": // Add a new league
					addLEAGUE(conn);
					break;

				default:
					displayMenu();
					break;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		out.println("Done");
	}

	/**
	 * Attempt to open a connection to an embedded Derby database at the given URL.
	 * If the database does not exist, create it with empty tables.
	 * 
	 * @param url
	 * @return
	 */
	private static Connection getConnection(String url) {
		Driver driver = new EmbeddedDriver();

		// try to connect to an existing database
		Properties prop = new Properties();
		prop.put("create", "false");
		try {
			Connection conn = driver.connect(url, prop);
			return conn;
		} catch (SQLException e) {
			// database doesn't exist, so try creating it
			try {
				prop.put("create", "true");
				Connection conn = driver.connect(url, prop);
				createTables(conn);
				return conn;
			} catch (SQLException e2) {
				throw new RuntimeException("cannot connect to database", e2);
			}
		}
	}

	private static void displayMenu() {
		out.println("0: Quit");
		out.println("1: Reset tables");
		out.println("2: ListPlayers");
		out.println("3: Show League Status");
		out.println("4: Show Team Line-up");
		out.println("5: Update Line-up");
		out.println("6: Update Market Value");
		out.println("7: Add League");
		
	}

	private static String requestString(String prompt) {
		out.print(prompt);
		out.flush();
		return in.nextLine();
	}

	private static void createTables(Connection conn) {
		// First clean up from previous runs, if any
		dropTables(conn);

		// Now create the schema
		addTables(conn);
	}

	private static void doUpdate(Connection conn, String statement, String message) {
		try (Statement stmt = conn.createStatement()) {
			stmt.executeUpdate(statement);
			System.out.println(message);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private static void doUpdateNoError(Connection conn, String statement, String message) {
		try (Statement stmt = conn.createStatement()) {
			stmt.executeUpdate(statement);
			System.out.println(message);
		} catch (SQLException e) {
			// Ignore error
		}
	}

	/**
	 * Create the tables for the Soccer League database from Sciore. Note that the tables
	 * have to be created in a particular order, so that foreign key references
	 * point to already-created tables. This allows the simpler technique of
	 * creating the tables directly with their f.k. constraints, rather than
	 * altering the tables later to add constraints.
	 * 
	 * @param conn
	 */
	private static void addTables(Connection conn) {
		StringBuilder sb = new StringBuilder();
		sb.append("create table LEAGUE(");
		sb.append("  league_id int,");
		sb.append("  lname varchar(50) not null,");
		sb.append("  country varchar(50) not null,");
		sb.append("  number_of_team int,");
		sb.append("  number_of_round int,");
		sb.append("  primary key (league_id)");
		sb.append(")");
		doUpdate(conn, sb.toString(), "Table LEAGUE created.");

		sb = new StringBuilder();
		sb.append("create table STADIUM(");
		sb.append("  stadium_id int,");
		sb.append("  Sname varchar(50) not null,");
		sb.append("  City varchar(50) not null,");
		sb.append("  Country varchar(50) not null,");
		sb.append("  primary key (stadium_id)                                                                                                                             ");
		sb.append(")");
		doUpdate(conn, sb.toString(), "Table STADIUM created.");

		sb = new StringBuilder();
		sb.append("create table TEAM(");
		sb.append("  team_id int,");
		sb.append("  tname varchar(50) not null,");
		sb.append("  manager varchar(50) not null,");
		sb.append("  stadium_id int not null,");
		sb.append("  league_id int not null,");
		sb.append("  primary key (team_id),");
		sb.append("  foreign key (league_id) references LEAGUE on delete cascade,");
		sb.append("  foreign key (stadium_id) references STADIUM on delete cascade");
		sb.append(")");
		doUpdate(conn, sb.toString(), "Table TEAM created.");

		sb = new StringBuilder();
		sb.append("create table PLAYER(");
		sb.append("  player_id int,");
		sb.append("  team_id int,");
		sb.append("  pname varchar(50) not null,");
		sb.append("  position varchar(5) not null,");
		sb.append("  country varchar(50) not null,");
		sb.append("  Transfermarkt_value int,");
		sb.append("  primary key (player_id),");
		sb.append("  foreign key (team_id) references TEAM on delete cascade");
		sb.append(")");
		doUpdate(conn, sb.toString(), "Table PLAYER created.");

	}

	/**
	 * Delete the tables for the student database. Note that the tables are dropped
	 * in the reverse order that they were created, to satisfy referential integrity
	 * (foreign key) constraints.
	 * 
	 * @param conn
	 */
	private static void dropTables(Connection conn) {
		doUpdateNoError(conn, "drop table LEAGUE", "Table LEAGUE dropped.");
		doUpdateNoError(conn, "drop table STADIUM", "Table STADIUM dropped.");
		doUpdateNoError(conn, "drop table TEAM", "Table TEAM dropped.");
		doUpdateNoError(conn, "drop table PLAYER", "Table PLAYER dropped.");
	
	}

	/**
	 * Delete the contents of the tables, then reinsert the sample data from Sciore.
	 * Again, note that the order is important, so that foreign key references
	 * already exist before they are used.
	 * 
	 * @param conn
	 */
	private static void resetTables(Connection conn) {
		try (Statement stmt = conn.createStatement()) {
			int count = 0;
			count += stmt.executeUpdate("delete from LEAGUE");
			count += stmt.executeUpdate("delete from STADIUM");
			count += stmt.executeUpdate("delete from TEAM");
			count += stmt.executeUpdate("delete from PLAYER");

			System.out.println(count + " records deleted");


			String[] LEAGUEvals = {
				"(1, 'EPL', 'England', 20, 38)", 
				"(2, 'La Liga', 'Spain', 20, 38)", 
				"(3, 'Bundesliga', 'Germany', 18, 34)", 
				"(4, 'Serie A', 'Italy', 20, 38)",
				"(5, 'Ligue 1', 'France', 20, 38)"
		};
			count = 0;
			for (String val : LEAGUEvals) {
				count += stmt.executeUpdate("insert into LEAGUE(league_id, lname, country, number_of_team, number_of_round) values " + val);
			}
			System.out.println(count + " LEAGUE records inserted.");


		String[] STADIUMvals = {
			//EPL
			"(100, 'Old Trafford', 'Manchester', 'England')",
			"(101, 'Anfield', 'Liverpool', 'England')",
			"(102, 'Emirates Stadium', 'London', 'England')",
			"(103, 'Etihad Stadium', 'Manchester', 'England')",
			"(104, 'Stamford Bridge', 'London', 'England')",
			"(105, 'Tottenham Hotspur Stadium', 'London', 'England')",
			"(106, 'Goodison Park', 'Liverpool', 'England')",
			"(107, 'King Power Stadium', 'Leicester', 'England')",

		
			//La Liga
			"(201, 'Camp Nou', 'Barcelona', 'Spain')",
			"(202, 'Santiago Bernabeu', 'Madrid', 'Spain')",
			"(203, 'Wanda Metropolitano', 'Madrid', 'Spain')",
			"(204, 'San Mames', 'Bilbao', 'Spain')",
			"(205, 'Mestalla Stadium', 'Valencia', 'Spain')",
			"(206, 'Sanchez Pizjuan ', 'Seville', 'Spain')",
	

	
			//Bundesliga
			"(301, 'Allianz Arena', 'Munich', 'Germany')",
			"(302, 'Signal Iduna Park', 'Dortmund', 'Germany')",
			"(303, 'Mercedes-Benz Arena', 'Stuttgart', 'Germany')",
			"(304, 'Olympiastadion Berlin', 'Berlin', 'Germany')",
			"(305, 'Volkswagen Arena', 'Wolfsburg', 'Germany')",
			"(306, 'RheinEnergieStadion', 'Cologne', 'Germany')",
			"(307, 'Red Bull Arena', 'Leipzig', 'Germany')",
			"(308, 'BayArena', 'Leverkusen', 'Germany')",


			//Italy
			"(400, 'San Siro', 'Milan', 'Italy')",
			"(401, 'Allianz Stadium', 'Turin', 'Italy')",
			"(402, 'Stadio Olimpico', 'Rome', 'Italy')",
			"(403, 'Stadio San Paolo', 'Naples', 'Italy')",
			"(404, 'Stadio Artemio Franchi', 'Florence', 'Italy')",



			//France
			"(500, 'Parc des Princes', 'Paris', 'France')",
			"(501, 'Stade Vélodrome', 'Marseille', 'France')",
			"(502, 'Groupama Stadium', 'Lyon', 'France')",
			"(503, 'Stade Geoffroy-Guichard', 'Saint-Étienne', 'France')",
			"(504, 'Roazhon Park', 'Rennes', 'France')",
			"(505, 'Stade de la Beaujoire', 'Nantes', 'France')",
			"(506, 'Matmut Atlantique', 'Bordeaux', 'France')",
			"(507, 'Allianz Riviera', 'Nice', 'France')",
			"(508, 'Stade Pierre-Mauroy', 'Lille', 'France')",
			"(509, 'Stade Louis II', 'Monaco', 'France')"

		};
		count = 0;
		for (String val : STADIUMvals) {
			count += stmt.executeUpdate("insert into STADIUM(stadium_id, SName, City, Country) values " + val);
		}
		System.out.println(count + " STADIUM records inserted.");
				
        String[] TEAMvals = {
			"(100, 'Manchester United', 'Erik Ten Hag', 100, 1)",
			"(101, 'Liverpool', 'Jurgen Klopp', 101, 1)",
			"(104, 'Chelsea', 'Frank Lampard', 104, 1)",
			"(102, 'Arsenal', 'Mikel Arteta', 102, 1)",
			"(105, 'Tottenham Hotspur', 'Ryan Mason', 105, 1)",
			"(103, 'Manchester City', 'Pep Guardiola', 103, 1)",

			"(200, 'Barcelona', 'Xavi Hernandez', 201, 2)",
			"(201, 'Real Madrid', 'Carlo Ancelotti', 202, 2)",
			"(202, 'Atletico Madrid', 'Diego Simeone', 203, 2)",
			"(203, 'Sevilla', 'José Luis Mendilibar', 206, 2)",
			"(204, 'Athletic Bilbao', 'Ernesto Valverde', 204, 2)",

			"(301, 'Bayern Munich', 'Thomas Tuchel', 301, 3)",
			"(302, 'Borussia Dortmund', 'Edin Terzic', 302, 3)",
			"(303, 'RB Leipzig', 'Jesse Marsch', 307, 3)",
			"(304, 'Bayer Leverkusen', 'Xabi Alonso', 308, 3)",
			"(305, 'Wolfsburg', 'Niko Kovac', 305, 3)",
			
			"(401, 'Juventus', 'Max Allegri', 401, 4)",
			"(402, 'Inter Milan', 'Filippo Inzaghi', 400, 4)",
			"(403, 'AC Milan', 'Stefano Pioli', 400, 4)",
			"(404, 'AS Roma', 'Jose Mourinho', 402, 4)",
			"(405, 'SSC Napoli', 'Gennaro Gattuso', 403, 4)",
			"(406, 'S,S. Lazio', 'Maurizio Sarri', 402, 4)",

			"(500, 'Paris Saint-Germain', 'Christophe Galtier', 500, 5)",
			"(501, 'Lille', 'Christophe Galtier', 508, 5)",
			"(502, 'Olympique Lyonnais', 'Laurent Blanc', 502, 5)",
			"(503, 'Marseille', 'Igor Tudor', 503, 5)",
			"(504, 'Monaco', 'Philippe Clement', 509, 5)"
		};
			for (String val : TEAMvals) {
				count += stmt.executeUpdate("insert into TEAM(team_id, tname, manager, stadium_id, league_id) values " + val);
			}
			System.out.println(count + " TEAM records inserted.");
			

			String[] PLAYERvals = {
				"(1, 105, 'Harry Kane', 'FW', 'England', 120000000)",
				"(2, 105, 'Heung-min Son', 'FW', 'South Korea', 90000000)",
				"(3, 105, 'Hugo Lloris', 'GK', 'France', 30000000)",
				"(4, 101, 'Mohamed Salah', 'FW', 'Egypt', 120000000)",
				"(5, 301, 'Sadio Mane', 'FW', 'Senegal', 90000000)",
				"(6, 101, 'Virgil van Dijk', 'DF', 'Netherlands', 100000000)",
				"(7, 103, 'Kevin De Bruyne', 'MF', 'Belgium', 120000000)",
				"(8, 104, 'Raheem Sterling', 'FW', 'England', 90000000)",
				"(9, 103, 'Ederson', 'GK', 'Brazil', 40000000)",
				"(10, 100, 'Cristiano Ronaldo', 'FW', 'Portugal', 80000000)",
				"(11, 404, 'Paulo Dybala', 'FW', 'Argentina', 80000000)",
				"(12, 401, 'Leonardo Bonucci', 'DF', 'Italy', 50000000)",
				"(13, 500, 'Kylian Mbappe', 'FW', 'France', 200000000)",
				"(14, 500, 'Neymar Jr.', 'FW', 'Brazil', 150000000)",
				"(15, 500, 'Keylor Navas', 'GK', 'Costa Rica', 10000000)",
				"(16, 103, 'Erling Haaland', 'FW', 'Norway', 120000000)",
				"(17, 302, 'Jude Bellingham', 'MF', 'England', 70000000)",
				"(18, 302, 'Gio Reyna', 'MF', 'United States', 60000000)",
				"(19, 500, 'Lionel Messi', 'FW', 'Argentina', 80000000)",
				"(20, 202, 'Memphis Depay', 'FW', 'Netherlands', 60000000)",
				"(21, 202, 'Antoine Griezmann', 'FW', 'France', 50000000)",
				"(22, 200, 'Robert Lewandowski', 'FW', 'Poland', 80000000)",
				"(23, 301, 'Thomas Muller', 'FW', 'Germany', 50000000)",
				"(24, 301, 'Manuel Neuer', 'GK', 'Germany', 30000000)",
				"(25, 402, 'Romelu Lukaku', 'FW', 'Belgium', 90000000)",
				"(26, 500, 'Achraf Hakimi', 'DF', 'Morocco', 50000000)",
				"(27, 402, 'Lautaro Martinez', 'FW', 'Argentina', 60000000)",
				"(28, 201, 'Luka Modric', 'MF', 'Croatia', 45000000)",
		        "(29, 301, 'Leroy Sane', 'LW', 'Germany', 80000000)",
				"(30, 103, 'Ilkay Gundogan', 'CM', 'Germany', 40000000)",
				"(31, 103, 'Bernardo Silva', 'RW', 'Portugal', 60000000)",
				"(32, 500, 'Gianluigi Donnarumma', 'GK', 'Italy', 70000000)",
				"(33, 406, 'Alessio Romagnoli', 'CB', 'Italy', 30000000)",
				"(34, 402, 'Hakan Calhanoglu', 'AM', 'Turkey', 50000000)",
				"(35, 403, 'Zlatan Ibrahimovic', 'ST', 'Sweden', 10000000)",
				"(36, 403, 'Olivier Giroud', 'ST', 'France', 10000000)",
				"(37, 100, 'Casemiro', 'MF', 'Brazil', 70000000)",
				"(38, 102, 'Martin Odegaard', 'MF', 'Norway', 70000000)",
				"(39, 401, 'Angel Di Maria', 'RW', 'Argentina', 40000000)",
				"(40, 500, 'Marco Verratti', 'CM', 'Italy', 60000000)",
				"(41, 202, 'Jan Oblak', 'GK', 'Slovenia', 90000000)",
				"(42, 500, 'Sergio Ramos', 'CB', 'Spain', 34000000)",
				"(43, 201, 'Karim Benzema', 'CF', 'France', 70000000)",
				"(44, 201, 'Toni Kroos', 'CM', 'Germany', 60000000)",
				"(45, 100, 'Bruno Fernandes', 'AM', 'Portugal', 70000000)",
				"(46, 100, 'Marcus Rashford', 'LW', 'England', 60000000)",
				"(47, 401, 'Paul Pogba', 'CM', 'France', 80000000)",
				"(48, 104, 'Alexis Sánchez', 'FW', 'Chile', 4500000)",
				"(49, 104, 'Victor Osimhen', 'CF', 'Nigeria', 100000000)",
				"(50, 104, 'Kalidou Koulibaly', 'CB', 'Senegal', 40000000)"
			};


			for (String val : PLAYERvals) {
				count += stmt.executeUpdate("insert into PLAYER(player_id, team_id, pname, position, country, Transfermarkt_value) values " + val);
			}
			System.out.println(count + " PLAYER records inserted.");

		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Print a table of all players with their id number, name, position, team, league played and transfermarkt_value
	 * 
	 * 
	 * @param conn
	 */
	private static void listPlayers(Connection conn) {
		StringBuilder query = new StringBuilder();
		
		query.append("select p.player_id, p.pname, p.position, t.tname, l.lname, p.Transfermarkt_value");
		query.append("  from PLAYER p, TEAM t, LEAGUE l");
		query.append("  where p.team_id = t.team_id");
		query.append("    and t.league_id = l.league_id");
		query.append("    order by p.player_id");

		try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query.toString())) {
			out.printf("%-1s %-5s %-2s %-5s %-2s %-6s \n", "playerID", "Name", "Position", "Team", "League", "Transfermarkt_value");
			out.println("----------------------------");
			while (rs.next()) {
				int pid = rs.getInt("player_id");
				String pname = rs.getString("pname");
				String position = rs.getString("position");
				String tname = rs.getString("tname");
                String lname = rs.getString("lname");
				int market_val = rs.getInt("Transfermarkt_value");
				out.printf("%-1d %-5s %-2s %-5s %-2s %-6d\n", pid, pname, position, tname, lname, market_val);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

	}


    /**
	 * Print a table of all players with their id number, name, position in a team. Team name is entered first
	 * 
	 * 
	 * @param conn
	 */	

	private static void listPlayersinTeam(Connection conn) {


		String lname = requestString("Team name? ");

		StringBuilder query = new StringBuilder();
		query.append("select p.player_id, p.pname, p.position, p.Transfermarkt_value");
		query.append("  from PLAYER p, TEAM t");
		query.append("  where p.team_id = t.team_id");
		query.append("    and t.tname = ?");
		query.append("    order by p.player_id");

		try (PreparedStatement pstmt = conn.prepareStatement(query.toString())) {
			pstmt.setString(1, lname);
			ResultSet rs = pstmt.executeQuery();

			out.printf("%-1s %-5s %-2s %-6s \n", "playerID", "Name", "Position", "Transfermarkt_value");
			out.println("----------------------------");
			while (rs.next()) {
				int pid = rs.getInt("player_id");
				String pname = rs.getString("pname");
				String position = rs.getString("position");
				int market_val = rs.getInt("Transfermarkt_value");

				out.printf("%-1d %-5s %-2s %-6d\n", pid, pname, position, market_val);
			}

			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}







	/**
	 * Request a League name and print a table of teams in that League with their home stadium and manager
	 * @param conn
	 */
	private static void LeagueInfomation(Connection conn) {



		String lname = requestString("League name? ");

		StringBuilder query = new StringBuilder();
		query.append("select t.team_id, t.tname, s.Sname, t.manager");
		query.append("  from TEAM t, STADIUM s, LEAGUE l");
		query.append("  where t.league_id = l.league_id");
		query.append("    and t.stadium_id = s.stadium_id");
		query.append("    and l.lName = ?");

		try (PreparedStatement pstmt = conn.prepareStatement(query.toString())) {
			pstmt.setString(1, lname);
			ResultSet rs = pstmt.executeQuery();

			out.printf("%-3s %-5s %-5s %-8s\n", "Team_Id", "Team", "Stadium", "Manager");
			out.println("-----------------------------------------------------");
			while (rs.next()) {
				int team_id = rs.getInt("team_id");
				String tname = rs.getString("tname");
				String stadium = rs.getString("Sname");
				String manager = rs.getString("manager");

				out.printf("%3d %-5s %-5s %-8s\n", team_id, tname, stadium, manager);
			}

			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Request information to update line-Up (add player(s)) to the team. The id number must
	 * be unique, and the team updated must be an existing team.
	 * @param conn
	 */
	private static void updateLineUp(Connection conn) {
		String pid = requestString("Player Id? ");
		String pname = requestString("Name? ");
		String pos = requestString("Position? ");
		String country = requestString("Country? ");
		String markt = requestString("Transfermarkt Value? ");
		String team = requestString("Team? ");


		StringBuilder command = new StringBuilder();
		command.append("insert into PLAYER(player_id, team_id, pname, position, country, Transfermarkt_value)");
		command.append("  select ?, t.team_id, ?, ?, ?, ?");
		command.append("  from TEAM t");
		command.append("  where t.tname = ?");


		
		try (PreparedStatement pstmt = conn.prepareStatement(command.toString())) {
			pstmt.setString(1, pid);
			pstmt.setString(2, pname);
			pstmt.setString(3, pos);
			pstmt.setString(4, country);
			pstmt.setString(5, markt);
			pstmt.setString(6, team);
			int count = pstmt.executeUpdate();

			out.println(count + " player(s) inserted");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Request an player id and a new market value to be entered, then update the
	 * enrollment table accordingly.
	 * 
	 * @param conn
	 */
	private static void changeMarketvalue(Connection conn) {
		String Transfermarkt_value = requestString("New Market value? ");
		String player_id = requestString("Player id number? ");

		StringBuilder command = new StringBuilder();
		command.append("update PLAYER p");
		command.append("  set Transfermarkt_value = ?");
		command.append("  where player_id = ?");

		try (PreparedStatement pstmt = conn.prepareStatement(command.toString())) {
			pstmt.setString(1, Transfermarkt_value);
			pstmt.setString(2, player_id) ;

			int count = pstmt.executeUpdate();

			out.println(count + " record(s) updated");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}



	/**
	 * Request information to add a new Leauge System to the database. The id number must
	 * be unique.
	 * @param conn
	 */
	private static void addLEAGUE(Connection conn) {
		String league_id = requestString("Id LEAGUE? ");
		String lname = requestString("LEAGUE name? ");
		String country = requestString("Country? ");
		String number_of_team = requestString("Number of teams? ");
    	String number_of_round = requestString("Number of rounds? ");

		StringBuilder command = new StringBuilder();
		command.append("insert into LEAGUE(league_id, lname, country, number_of_team, number_of_round) ");
		command.append("  values ( ?, ?, ?, ?, ?)");
	


		try (PreparedStatement pstmt = conn.prepareStatement(command.toString())) {
			pstmt.setString(1, league_id);
			pstmt.setString(2, lname);
			pstmt.setString(3, country);
			pstmt.setString(4, number_of_team);
			pstmt.setString(5, number_of_round);
			int count = pstmt.executeUpdate();

			out.println(count + " league(s) inserted");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
