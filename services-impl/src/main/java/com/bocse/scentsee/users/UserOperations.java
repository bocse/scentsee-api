package com.bocse.scentsee.users;

import com.aerospike.client.AerospikeClient;
import org.joda.time.DateTime;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Created by bocse on 02.03.2016.
 */

public class UserOperations {

    private AerospikeClient client;
    private UserManagement userManagement;

    public UserOperations() {
        //init();
    }

    public static void main(String[] args) {

        UserOperations userOperations = new UserOperations();
        userOperations.init(args);
        userOperations.choices();
    }

    public void init(String[] args) {
        if (args.length == 0) {
            client = new AerospikeClient("78.46.238.82", 31379);
        } else {
            client = new AerospikeClient(args[0], Integer.valueOf(args[1]));
        }
        userManagement = new UserManagement(client, "scentsee");
    }

    private void createUser() {

        Scanner scanIn = new Scanner(System.in);
        System.out.println("Email: ");

        String email = scanIn.nextLine().trim().toLowerCase();
        System.out.println("Password: ");
        String password = scanIn.nextLine();
        System.out.println("First Name: ");
        String firstName = scanIn.nextLine().trim();
        System.out.println("Last Name: ");
        String lastName = scanIn.nextLine().trim();
        System.out.println("Stock Auth (comma separated): ");
        String stockAuthString = scanIn.nextLine().trim().toLowerCase();
        List<String> stockAuth = Arrays.asList(stockAuthString.split(","));


        //String password = "ScentSee.com";

        User user = new User();
        user.setEmail(email);
        user.setPasswordWithHash(password);
        user.setLimit(1500L);
        user.setActive(true);
        user.setInterval(3600 * 1000L);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setCreationTimestamp(System.currentTimeMillis());
        for (String stockAuthItem : stockAuth) {
            if (!stockAuthItem.trim().isEmpty()) {
                user.getStockAuthorization().add(stockAuthItem.trim());
            }
        }

        userManagement.createUser(user, true);
    }

    private void deleteByEmail() {
        Scanner scanIn = new Scanner(System.in);
        System.out.println("Email: ");

        String email = scanIn.nextLine().trim().toLowerCase();

        User user;
        do {
            user = userManagement.getUserByEmail(email);
            if (user != null) {
                userManagement.deleteUserByAccessKey(user.getAccessKey());
                System.out.println("Deleted.");
            } else {
                break;
            }
        } while (user != null);

    }

    private void deleteByAccessKey() {
        Scanner scanIn = new Scanner(System.in);
        System.out.println("AccessKey: ");

        String accessKey = scanIn.nextLine().trim();
        userManagement.deleteUserByAccessKey(accessKey);
        System.out.println("Deleted.");

    }

    private void showUsers() {
        System.out.println("Querying users");
        List<User> users = userManagement.getAllUsers();
        if (users.isEmpty())
            System.out.println("No users found.");
        for (User user : users) {
            System.out.println(
                    user.getEmail() + "\t" +
                            user.getActive() + "\t" +
                            user.getAccessKey() + "\t" +
                            user.getSecretKey() + "\t" +
                            user.getStockAuthorization() + "\t" +
                            user.getLimit() + "\t" +
                            new DateTime(user.getCreationTimestamp())
            );
        }
    }

    public void choices() {

        System.out.println("1 - Create user");
        System.out.println("2 - Delete user by email");
        System.out.println("3 - Delete user by access key");
        System.out.println("4 - Show users");
        System.out.println("0 - Exit");
        Scanner scanIn = new Scanner(System.in);
        System.out.println("Choice: ");
        int choice = scanIn.nextInt();
        switch (choice) {
            case 1:
                createUser();
                break;
            case 2:
                deleteByEmail();
                break;
            case 3:
                deleteByAccessKey();
                break;
            case 4:
                showUsers();
                break;
            case 0:
                client.close();
                System.out.println("Exiting.");
                return;
            default:
                System.out.println("Unknown option");

        }
    }
}
