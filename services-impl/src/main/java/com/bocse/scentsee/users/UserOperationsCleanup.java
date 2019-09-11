package com.bocse.scentsee.users;

import com.aerospike.client.AerospikeClient;

import java.util.Scanner;

/**
 * Created by bocse on 02.03.2016.
 */

public class UserOperationsCleanup {


    public static void main(String[] args) {
        AerospikeClient client = new AerospikeClient("78.46.238.82", 31379);
        UserManagement userManagement = new UserManagement(client, "scentsee");
        //User oldUser=userManagement.getUserByEmail("olesea.harea@committed.ro");
        //System.out.println(oldUser.getAccessKey());
        //userManagement.deleteUserByAccessKey(oldUser.getAccessKey());
        //System.exit(0);
//        User user=userManagement.getUserByEmail("bogdan.bocse@gmail.com");
//        System.out.println(userManagement.deleteUserByAccessKey(user.getAccessKey()));
//        System.out.println(user.getAccessKey());
        Scanner scanIn = new Scanner(System.in);
        System.out.println("Email: ");

        String email = scanIn.nextLine().trim().toLowerCase();

        User user;
        do {
            user = userManagement.getUserByEmail(email);

            if (!user.getAccessKey().equals("gvfZD7YgI5AWmsabcMelPJgqP1ycD1Ke")) {
                System.out.println("Delete user with access Key" + user.getAccessKey());
                String answer = scanIn.nextLine().trim().toLowerCase();
                if (answer.equals("y")) {
                    userManagement.deleteUserByAccessKey(user.getAccessKey());
                }
            }
        }
        while (user != null);
    }
}
