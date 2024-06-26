package edu.virginia.sde.reviews;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseDriver {
    private final String sqliteFilename;
    protected Connection connection;

    public DatabaseDriver (String sqlListDatabaseFilename) {
        this.sqliteFilename = sqlListDatabaseFilename;
    }

    /**
     * Connect to a SQLite Database. This turns out Foreign Key enforcement, and disables auto-commits
     */
    public void connect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            throw new IllegalStateException("The connection is already opened");
        }
        connection = DriverManager.getConnection("jdbc:sqlite:" + sqliteFilename);
        //the next line enables foreign key enforcement - do not delete/comment out
        connection.createStatement().execute("PRAGMA foreign_keys = ON");
        //the next line disables auto-commit - do not delete/comment out
        connection.setAutoCommit(false);
    }

    /**
     * Commit all changes since the connection was opened OR since the last commit/rollback
     */
    public void commit() throws SQLException {
        connection.commit();
    }

    /**
     * Rollback to the last commit, or when the connection was opened
     */
    public void rollback() throws SQLException {
        connection.rollback();
    }

    /**
     * Ends the connection to the database
     */
    public void disconnect() throws SQLException {
        connection.close();
    }


    /**
     * Creates Users, Courses, and Reviews tables for database
     */
    public void createTables() throws SQLException {
        if(connection.isClosed()) {
            throw new IllegalStateException("Connection is not open.");
        }
        // Agent: ChatGPT
        // Usage: Asked how generate certain SQL commands (i.e. based on how to introduce foreign keys)
        connection.createStatement().execute("CREATE TABLE IF NOT EXISTS Users (" +
                "UserID INTEGER PRIMARY KEY," +
                "Username TEXT UNIQUE NOT NULL," +
                "Password TEXT NOT NULL" +
                ");");
        connection.createStatement().execute("CREATE TABLE IF NOT EXISTS Courses (" +
                "CourseID INTEGER PRIMARY KEY," +
                "Mnemonic TEXT NOT NULL," +
                "CourseNumber INTEGER NOT NULL," +
                "CourseName TEXT NOT NULL," +
                "AverageRating REAL DEFAULT 0.0" +
                ");");
        connection.createStatement().execute("CREATE TABLE IF NOT EXISTS Reviews (" +
                "ReviewID INTEGER PRIMARY KEY," +
                "UserID INTEGER," +
                "CourseID INTEGER," +
                "Rating REAL NOT NULL," +
                "Comment TEXT," +
                "ReviewTimestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"+ //unsure of how to use this data type
                "FOREIGN KEY(UserID) REFERENCES Users(UserID) ON DELETE CASCADE," +
                "FOREIGN KEY(CourseID) REFERENCES Courses(CourseID) ON DELETE CASCADE" +
                ");");
    }

    /**
     * Adds users to the Users table
     */
    public void addUser(User user) throws SQLException{
        if(connection.isClosed()) {
            throw new IllegalStateException("Connection is not open.");
        }
        try{
            var insertUserQuery = "INSERT INTO Users (Username, Password) VALUES (?, ?)";
            var userStatement = connection.prepareStatement(insertUserQuery);

            userStatement.setString(1, user.getUsername());
            userStatement.setString(2, user.getPassword());
            userStatement.executeUpdate();
            userStatement.close();
        } catch (SQLException e){
            rollback();
            throw e;
        }
    }

    /**
     * Adds courses to the Courses table
     */
    public void addCourse(Course course) throws SQLException{
        if(connection.isClosed()) {
            throw new IllegalStateException("Connection is not open.");
        }
        try{
            var insertCourseQuery = "INSERT INTO Courses (Mnemonic, CourseNumber, CourseName, AverageRating) VALUES (?, ?, ?, ?)";
            var courseStatement = connection.prepareStatement(insertCourseQuery);

            courseStatement.setString(1, course.getMnemonic());
            courseStatement.setInt(2, course.getCourseNumber());
            courseStatement.setString(3, course.getCourseName());
            courseStatement.setDouble(4, course.getAverageRating());
            courseStatement.executeUpdate();
            courseStatement.close();
        } catch (SQLException e){
            rollback();
            throw e;
        }
    }

    /**
     * Adds reviews to the Reviews table
     */
    public void addReview(Review review) throws SQLException{
        if(connection.isClosed()) {
            throw new IllegalStateException("Connection is not open.");
        }
        try{
            var insertReviewQuery = "INSERT INTO Reviews (UserID, CourseID, Rating, Comment, ReviewTimestamp) VALUES (?, ?, ?, ?, ?)";
            var reviewStatement = connection.prepareStatement(insertReviewQuery);

                reviewStatement.setInt(1, review.getUserID());
                reviewStatement.setInt(2, review.getCourseID());
                reviewStatement.setDouble(3, review.getRating());
                reviewStatement.setString(4, review.getComment());
                reviewStatement.setTimestamp(5, review.getTimestamp());
                reviewStatement.executeUpdate();
                reviewStatement.close();
        } catch (SQLException e){
            rollback();
            throw e;
        }
    }

    /**
     * Returns all users in the Users table
     */
    public List<User> getAllUsers() throws SQLException{
        if(connection.isClosed()) {
            throw new IllegalStateException("Connection is not open.");
        }
        List<User> userList = new ArrayList<>();
        var statement = connection.prepareStatement("SELECT * FROM Users");

        var resultSet = statement.executeQuery();
        while(resultSet.next()){
            var username = resultSet.getString("Username");
            var password = resultSet.getString("Password");
            var newUser = new User(username, password);

            userList.add(newUser);
        }
        statement.close();
        return userList;
    }

    /**
     * Returns all courses in the Courses table
     */
    public List<Course> getAllCourses() throws SQLException{
        if(connection.isClosed()) {
            throw new IllegalStateException("Connection is not open.");
        }
        List<Course> courseList = new ArrayList<>();
        var statement = connection.prepareStatement("SELECT * FROM Courses");
        var resultSet = statement.executeQuery();

        while(resultSet.next()){
            var mnemonic = resultSet.getString("Mnemonic");
            var courseNumber = resultSet.getInt("CourseNumber");
            var courseName = resultSet.getString("CourseName");
            var averageRating = resultSet.getDouble("AverageRating");
            var newCourse = new Course(mnemonic, courseNumber, courseName, averageRating);

            courseList.add(newCourse);
        }
        statement.close();
        return courseList;
    }
    public List<Course> getSearchedCourses(String subject, String number, String title) throws SQLException {
        if (connection.isClosed()) {
            throw new IllegalStateException("Connection is not open.");
        }
        ResultSet resultSet;
        List<Course> courseList = new ArrayList<>();
        //all fields provided
        if(!subject.isEmpty() && !number.isEmpty() && !title.isEmpty()) {
            String sql = "SELECT * FROM Courses WHERE Mnemonic = ? AND CourseNumber = ? AND CourseName LIKE ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, subject);
                statement.setString(2, number);
                statement.setString(3, "%" + title + "%");
                resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    String mnemonic = resultSet.getString("Mnemonic");
                    int courseNumber = resultSet.getInt("CourseNumber");
                    String courseName = resultSet.getString("CourseName");
                    double averageRating = resultSet.getDouble("AverageRating");

                    Course newCourse = new Course(mnemonic, courseNumber, courseName, averageRating);
                    courseList.add(newCourse);
                }
            }
        }
        //only subject + number provided
        else if(!subject.isEmpty() && !number.isEmpty()) {
            String sql = "SELECT * FROM Courses WHERE Mnemonic = ? AND CourseNumber = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, subject);
                statement.setString(2, number);
                resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    String mnemonic = resultSet.getString("Mnemonic");
                    int courseNumber = resultSet.getInt("CourseNumber");
                    String courseName = resultSet.getString("CourseName");
                    double averageRating = resultSet.getDouble("AverageRating");

                    Course newCourse = new Course(mnemonic, courseNumber, courseName, averageRating);
                    courseList.add(newCourse);
                }
            }
        }
        //only subject + title provided
        else if(!subject.isEmpty() && !title.isEmpty()) {
            String sql = "SELECT * FROM Courses WHERE Mnemonic = ? AND CourseName LIKE ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, subject);
                statement.setString(2, "%" + title + "%");
                resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    String mnemonic = resultSet.getString("Mnemonic");
                    int courseNumber = resultSet.getInt("CourseNumber");
                    String courseName = resultSet.getString("CourseName");
                    double averageRating = resultSet.getDouble("AverageRating");

                    Course newCourse = new Course(mnemonic, courseNumber, courseName, averageRating);
                    courseList.add(newCourse);
                }
            }
        }
        //only number + title provided
        else if(subject.isEmpty() && !number.isEmpty() && !title.isEmpty()) {
            String sql = "SELECT * FROM Courses WHERE CourseNumber = ? AND CourseName LIKE ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, number);
                statement.setString(2, "%" + title + "%");
                resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    String mnemonic = resultSet.getString("Mnemonic");
                    int courseNumber = resultSet.getInt("CourseNumber");
                    String courseName = resultSet.getString("CourseName");
                    double averageRating = resultSet.getDouble("AverageRating");

                    Course newCourse = new Course(mnemonic, courseNumber, courseName, averageRating);
                    courseList.add(newCourse);
                }
            }
        }
        //only subject provided
        else if(!subject.isEmpty()) {
            String sql = "SELECT * FROM Courses WHERE Mnemonic = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, subject);
                resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    String mnemonic = resultSet.getString("Mnemonic");
                    int courseNumber = resultSet.getInt("CourseNumber");
                    String courseName = resultSet.getString("CourseName");
                    double averageRating = resultSet.getDouble("AverageRating");

                    Course newCourse = new Course(mnemonic, courseNumber, courseName, averageRating);
                    courseList.add(newCourse);
                }
            }
        }
        //only number provided
        else if(!number.isEmpty()) {
            String sql = "SELECT * FROM Courses WHERE CourseNumber = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, number);
                resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    String mnemonic = resultSet.getString("Mnemonic");
                    int courseNumber = resultSet.getInt("CourseNumber");
                    String courseName = resultSet.getString("CourseName");
                    double averageRating = resultSet.getDouble("AverageRating");

                    Course newCourse = new Course(mnemonic, courseNumber, courseName, averageRating);
                    courseList.add(newCourse);
                }
            }
        }
        //only title provided
        else if(!title.isEmpty()) {
            String sql = "SELECT * FROM Courses WHERE CourseName LIKE ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, "%" + title + "%");
                resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    String mnemonic = resultSet.getString("Mnemonic");
                    int courseNumber = resultSet.getInt("CourseNumber");
                    String courseName = resultSet.getString("CourseName");
                    double averageRating = resultSet.getDouble("AverageRating");

                    Course newCourse = new Course(mnemonic, courseNumber, courseName, averageRating);
                    courseList.add(newCourse);
                }
            }
        }
        return courseList;
    }


    /**
     * Returns all reviews in the Reviews table
     */
    public List<Review> getAllReviews() throws SQLException{
        if(connection.isClosed()) {
            throw new IllegalStateException("Connection is not open.");
        }
        List<Review> reviewsList = new ArrayList<>();
        var statement = connection.prepareStatement("SELECT * FROM Reviews");
        var resultSet = statement.executeQuery();

        while(resultSet.next()){
           var userID = resultSet.getInt("UserID");
           var courseID = resultSet.getInt("CourseID");
           var rating = resultSet.getInt("Rating");
           var comment = resultSet.getString("Comment");
           var timestamp = resultSet.getTimestamp("ReviewTimestamp");
           var newReview = new Review(userID, courseID, rating, comment, timestamp);

            reviewsList.add(newReview);
        }
        statement.close();
        return reviewsList;
    }

    /**
     * Gets all reviews for a given user
     */
    public List<Review> getReviewsByUser(User user) throws SQLException {
        if(connection.isClosed()) {
            throw new IllegalStateException("Connection is not open.");
        }
        PreparedStatement statement = connection.prepareStatement("""
            SELECT * FROM Reviews
            WHERE UserID IN(
            SELECT UserID FROM Users WHERE Username = ?)
            """);
        var reviewsList = new ArrayList<Review>();
        statement.setString(1, user.getUsername());
        ResultSet resultSet = statement.executeQuery();

        while(resultSet.next()) {
            var userID = resultSet.getInt("UserID");
            var courseID = resultSet.getInt("CourseID");
            var rating = resultSet.getInt("Rating");
            var comment = resultSet.getString("Comment");
            var timestamp = resultSet.getTimestamp("ReviewTimestamp");

            var review = new Review(userID, courseID, rating, comment, timestamp);
            reviewsList.add(review);
        }
        statement.close();
        return reviewsList;
    }

    /**
     * Gets all reviews for a given course
     */
    public List<Review> getReviewsByCourse(Course course) throws SQLException {
        if(connection.isClosed()) {
            throw new IllegalStateException("Connection is not open.");
        }
        var reviewsList = new ArrayList<Review>();
        PreparedStatement statement = connection.prepareStatement("""
                                SELECT * FROM Reviews
                                JOIN Courses ON Reviews.CourseID = Courses.CourseID
                                WHERE Mnemonic = ? AND CourseNumber = ? AND CourseName = ?
                                """);
        statement.setString(1, course.getMnemonic());
        statement.setInt(2, course.getCourseNumber());
        statement.setString(3, course.getCourseName());
        ResultSet resultSet = statement.executeQuery();

        while(resultSet.next()) {
            var userID = resultSet.getInt("UserID");
            var courseID = resultSet.getInt("CourseID");
            var rating = resultSet.getInt("Rating");
            var comment = resultSet.getString("Comment");
            var timestamp = resultSet.getTimestamp("ReviewTimestamp");

            var review = new Review(userID, courseID, rating, comment, timestamp);
            reviewsList.add(review);
        }
        statement.close();
        return reviewsList;
    }

    /**
     * Gets a list of all courses with a course name that contains a given substring (case-insensitive)
     */
    public List<Course> getCourseByName(String substring) throws SQLException {
        if(connection.isClosed()) {
            throw new IllegalStateException("Connection is not open");
        }
        var coursesList = new ArrayList<Course>();
        PreparedStatement statement = connection.prepareStatement("SELECT * FROM Courses WHERE CourseName LIKE ?");
        substring = "%" + substring + "%";
        statement.setString(1, substring);
        ResultSet resultSet = statement.executeQuery();

        while(resultSet.next()) {
            var mnemonic = resultSet.getString("Mnemonic");
            var courseNumber = resultSet.getInt("CourseNumber");
            var courseName = resultSet.getString("CourseName");
            var averageRating = resultSet.getDouble("AverageRating");
            var course = new Course(mnemonic, courseNumber, courseName, averageRating);
            coursesList.add(course);
        }

        statement.close();
        return coursesList;
    }

    /**
     * Gets a list of all courses that match a given mnemonic
     */
    public List<Course> getCourseByMnemonic(String courseMnemonic) throws SQLException {
        if(connection.isClosed()) {
            throw new IllegalStateException("Connection is not open");
        }
        var coursesList = new ArrayList<Course>();
        PreparedStatement statement = connection.prepareStatement("SELECT * FROM Courses WHERE Mnemonic = ?");
        statement.setString(1, courseMnemonic);
        ResultSet resultSet = statement.executeQuery();

        while(resultSet.next()) {
            var mnemonic = resultSet.getString("Mnemonic");
            var courseNumber = resultSet.getInt("CourseNumber");
            var courseName = resultSet.getString("CourseName");
            var averageRating = resultSet.getDouble("AverageRating");
            var course = new Course(mnemonic, courseNumber, courseName, averageRating);
            coursesList.add(course);
        }
        statement.close();
        return coursesList;
    }

    /**
     * Gets a list of courses that match a given course number
     */

    public List<Course> getCourseByNumber(int number) throws SQLException {
        if(connection.isClosed()) {
            throw new IllegalStateException("Connection is not open");
        }
        var coursesList = new ArrayList<Course>();
        PreparedStatement statement = connection.prepareStatement("SELECT * FROM Courses WHERE CourseNumber = ?");
        statement.setInt(1, number);
        ResultSet resultSet = statement.executeQuery();

        while(resultSet.next()) {
            var mnemonic = resultSet.getString("Mnemonic");
            var courseNumber = resultSet.getInt("CourseNumber");
            var courseName = resultSet.getString("CourseName");
            var averageRating = resultSet.getDouble("AverageRating");
            var course = new Course(mnemonic, courseNumber, courseName, averageRating);
            coursesList.add(course);
        }
        statement.close();
        return coursesList;
    }

    /**
     * Gets the course corresponding to given course ID
     */

    public Course getCourseByCourseID(int courseID) throws SQLException{
        if(connection.isClosed()) {
            throw new IllegalStateException("Connection is not open");
        }
        PreparedStatement statement = connection.prepareStatement("SELECT * FROM Courses WHERE CourseID = ?");
        statement.setInt(1, courseID);
        ResultSet resultSet = statement.executeQuery();

        var mnemonic = resultSet.getString("Mnemonic");
        var courseNumber = resultSet.getInt("CourseNumber");
        var courseName = resultSet.getString("CourseName");
        var averageRating = resultSet.getDouble("AverageRating");
        return new Course(mnemonic, courseNumber, courseName, averageRating);
    }

    /**
     * Checks if username already exists in Users
     */
    public boolean checkUserExists(String username) throws SQLException{
        if(connection.isClosed()) {
            throw new IllegalStateException("Connection is not open.");
        }
        // Agent: ChatGPT
        // Source: Asked how to return boolean from searching a table
        var statement = connection.prepareStatement("SELECT * FROM Users WHERE Username = ?");
        statement.setString(1, username);
        var resultSet = statement.executeQuery();

        boolean hasUsername = resultSet.next();
        statement.close();
        return hasUsername; //returns true if there exists at a row w/the username
    }

    public boolean checkUserPassword(String username, String password) throws  SQLException{
        if(connection.isClosed()) {
            throw new IllegalStateException("Connection is not open.");
        }
        var statement = connection.prepareStatement("SELECT * FROM Users WHERE Username = ? AND Password = ?");
        statement.setString(1, username);
        statement.setString(2, password);

        var resultSet = statement.executeQuery();

        boolean hasPassword = resultSet.next();
        statement.close();
        return hasPassword; //returns true if there exists at a row w/the password
    }


    /**
     * Checks if course exists
     */
    public boolean checkCourseExists(String mnemonic, String courseNumber, String courseName) throws SQLException{
        if(connection.isClosed()) {
            throw new IllegalStateException("Connection is not open.");
        }
        var statement = connection.prepareStatement("SELECT * FROM Courses WHERE Mnemonic = ? AND CourseNumber = ? AND CourseName = ?");
        statement.setString(1, mnemonic);
        statement.setString(2, courseNumber);
        statement.setString(3, courseName);


        var resultSet = statement.executeQuery();

        boolean exists = resultSet.next();
        statement.close();
        return exists; //returns true if there exists at a row w/the password
    }

    /**
     * Checks to see if review exists in database
     */
    public boolean reviewExists(int userID, int courseID) throws SQLException{
        var statement = connection.prepareStatement("SELECT * FROM Reviews WHERE UserID = ? AND CourseID = ?");
        statement.setInt(1, userID);
        statement.setInt(2, courseID);
        var resultSet = statement.executeQuery();
        var reviewExists = resultSet.next();
        statement.close();

        return reviewExists;
    }

    /**
     * Used to delete review
     */
    public void deleteReview(int userID, int courseID) throws SQLException{
        try{
            if(reviewExists(userID, courseID)){
                //var ratingToDelete = 0 - getRatingForReview(userID, courseID);
                var deleteQuery = "DELETE FROM Reviews WHERE UserID = ? AND CourseID = ?";
                var deleteStatement = connection.prepareStatement(deleteQuery);

                deleteStatement.setInt(1, userID);
                deleteStatement.setInt(2, courseID);
                deleteStatement.executeUpdate();
                deleteStatement.close();

                updateAverageRating(courseID);
            }
        }
        catch(SQLException e){
            rollback();
            throw e;
        }
    }

    private int getRatingForReview(int userID, int courseID) throws SQLException{
        var statement = connection.prepareStatement("SELECT Rating FROM Reviews WHERE UserID = ? AND CourseID = ?");
        statement.setInt(1, userID);
        statement.setInt(2, courseID);
        var resultSet = statement.executeQuery();

        if(resultSet.next()){
            return resultSet.getInt("Rating");
        }else{
            return 0;
        }
    }


    /**
     * Removes all data from the tables, leaving the tables empty (but still existing!).
     */
    public void clearTables() throws SQLException {
        if(connection.isClosed()) {
            throw new IllegalStateException("Connection is not open.");
        }
        try {
            connection.createStatement().execute("DELETE FROM Reviews;");

            connection.createStatement().execute("DELETE FROM Courses;");

            connection.createStatement().execute("DELETE FROM Users;");


        } catch (SQLException e) {
            rollback();
            throw e;
        }
    }
    public boolean isValidPassword(String pass){
        if(pass.length() >= 8){
            return true;
        }
        else return false;
    }

    public int getCourseIDbyCourseTitleAndMnemonic(String courseTitle, String mnemonic, String courseNum) throws SQLException{
        if(connection.isClosed()) {
            throw new IllegalStateException("Connection is not open.");
        }
        try{
            var statement = connection.prepareStatement("SELECT CourseID FROM Courses WHERE CourseName = ? AND Mnemonic = ? AND CourseNumber = ?");
            statement.setString(1, courseTitle);
            statement.setString(2, mnemonic);
            statement.setString(3, courseNum);
            var resultSet = statement.executeQuery();

            if (resultSet.next()) {
                // Assuming CourseID is an integer column in your database
                return resultSet.getInt("CourseID");
            } else {
                // Handle the case when no result is found
                return -1; // Or some other appropriate value
            }
        } catch (SQLException e){
            rollback();
            throw e;
        }
    }

    public int getUserIDByUsername(String username) throws SQLException{
        if(connection.isClosed()) {
            throw new IllegalStateException("Connection is not open.");
        }
        try{
            var statement = connection.prepareStatement("SELECT UserID FROM Users WHERE Username = ?");
            statement.setString(1, username);
            var resultSet = statement.executeQuery();

            if (resultSet.next()) {
                // Assuming UserID is an integer column in your database
                return resultSet.getInt("UserID");
            } else {
                // Handle the case when no result is found
                return -1; // Or some other appropriate value
            }
        } catch (SQLException e){
            rollback();
            throw e;
        }

    }

    /**
     * Gets the sum of all ratings for a particular course
     */

    public int getSumOfRatings(int courseID) throws SQLException {
        if(connection.isClosed()) {
            throw new IllegalStateException("Connection is not open.");
        }
        var statement = connection.prepareStatement("SELECT Rating FROM Reviews WHERE CourseID = ?");
        statement.setInt(1, courseID);
        var resultSet = statement.executeQuery();
        int sum = 0;
        while(resultSet.next()) {
            sum += resultSet.getInt("Rating");
        }

        return sum;
    }

    public void updateAverageRating(int courseID) throws SQLException {
        int numReviews = getReviewsByCourse(getCourseByCourseID(courseID)).size();
        int sumRatings = getSumOfRatings(courseID);
        double newAverage = Math.round(((double) sumRatings / numReviews) * 100) / 100.0;
        var statement = connection.prepareStatement("UPDATE Courses SET AverageRating = ? WHERE CourseID = ?");
        statement.setDouble(1, newAverage);
        statement.setInt(2, courseID);
        statement.executeUpdate();
    }

    public boolean userIDAlreadyReviewedCourse(int userID, int courseID) throws SQLException{
        if(connection.isClosed()) {
            throw new IllegalStateException("Connection is not open.");
        }
        try{
            var statement = connection.prepareStatement("SELECT count(*) FROM Reviews WHERE UserID = ? AND CourseID = ?");
            statement.setString(1, String.valueOf(userID));
            statement.setString(2, String.valueOf(courseID));

            try (var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    // User has already reviewed an item
                    return resultSet.getInt(1) > 0;
                } else {
                    // User has not reviewed any item
                    return false;
                }
            }
        } catch (SQLException e){
            rollback();
            throw e;
        }
    }

    public Double loadRatingByUserID(int userID, int courseID) throws SQLException{
        if(connection.isClosed()) {
            connect();
        }

        try{
            var statement = connection.prepareStatement("SELECT Rating FROM Reviews WHERE UserID = ? AND CourseID = ?");
            statement.setString(1, String.valueOf(userID));
            statement.setString(2, String.valueOf(courseID));
            var resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getDouble("Rating");
            } else {
                return 0.0;
            }
        } catch (SQLException e){
            rollback();
            throw e;
        }
    }

    public String loadCommentByUserID(int userID, int courseID) throws SQLException{
        if(connection.isClosed()) {
            throw new IllegalStateException("Connection is not open.");
        }
        try{
            var statement = connection.prepareStatement("SELECT Comment FROM Reviews WHERE UserID = ? AND CourseID = ?");
            statement.setString(1, String.valueOf(userID));
            statement.setString(2, String.valueOf(courseID));
            var resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getString("Comment");
            } else {
                return null;
            }
        } catch (SQLException e){
            rollback();
            throw e;
        }
    }


    //update database with changes
    public void editReview(int userID, int courseID, String comment, String rating) throws SQLException{
        if(connection.isClosed()) {
            throw new IllegalStateException("Connection is not open.");
        }
        try{
            var statement = connection.prepareStatement("SELECT * FROM Reviews WHERE UserID = ? AND CourseID = ?");
            statement.setString(1, String.valueOf(userID));
            statement.setString(2, String.valueOf(courseID));
            var resultSet = statement.executeQuery();

            if (resultSet.next()) {
                var updateStatement = connection.prepareStatement("UPDATE Reviews SET Comment = ?, Rating = ? WHERE UserID = ? AND CourseID = ?");
                updateStatement.setString(1, comment);
                updateStatement.setString(2, rating);
                updateStatement.setInt(3, userID);
                updateStatement.setInt(4, courseID);

                updateStatement.executeUpdate();
            }

        } catch (SQLException e){
            rollback();
            throw e;
        }
    }

    public List<MyReviewsResult> getUserReviews(int userID) throws SQLException{
        if(connection.isClosed()) {
            throw new IllegalStateException("Connection is not open.");
        }
        var userReviews = new ArrayList<MyReviewsResult>();
        var statement = connection.prepareStatement("""
                             SELECT * FROM Reviews
                             JOIN Courses ON Reviews.CourseID = Courses.CourseID
                             WHERE UserID = ?
                             """);
        statement.setInt(1, userID);
        var resultSet = statement.executeQuery();
        while(resultSet.next()) {
            var rating = resultSet.getInt("Rating");
            var courseMnemonic = resultSet.getString("Mnemonic");
            var courseNumber = resultSet.getInt("CourseNumber");
            var courseID = resultSet.getInt("CourseID");
            var myReview = new MyReviewsResult(rating, courseMnemonic, courseNumber, courseID);
            userReviews.add(myReview);
        }
        return userReviews;
    }

    public Course getCourseForMyReviewResult(MyReviewsResult selectedReview) throws SQLException {
        if(connection.isClosed()){
            throw new IllegalStateException("Connection is not open.");
        }
        int courseID = selectedReview.getCourseID();
        return getCourseByCourseID(courseID);
    }
}
