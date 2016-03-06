/**
 This file is part of "Harta Banilor Publici".

 "Harta Banilor Publici" is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 "Harta Banilor Publici" is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.initiativaromania.hartabanilorpublici.IRUserInterface;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.initiativaromania.hartabanilorpublici.R;
import com.initiativaromania.hartabanilorpublici.IRObjects.Category;
import com.initiativaromania.hartabanilorpublici.IRObjects.CommManager;
import com.initiativaromania.hartabanilorpublici.IRObjects.Company;
import com.initiativaromania.hartabanilorpublici.IRObjects.Contract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;

/**
 * Created by claudiu on 2/10/16.
 */
public class ContractActivity extends Activity {
    public static final String EXTRA_CONTRACT_ID    = "com.example.claudiu.investitiipublice.IRObjects.Contract";
    private static final String JUSTIFY_PREFFERENCE = "justify_prefference";
    private static final int MINIMUM_VOTES          = 300;

    private Contract contract;
    private Context contractContext = this;
    private SharedPreferences just_prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contract);

        just_prefs = getApplicationContext().getSharedPreferences(JUSTIFY_PREFFERENCE, 0);

        /* Get the contract only with the id from the intent */
        Intent intent = getIntent();
        contract = (Contract)intent.getSerializableExtra(EXTRA_CONTRACT_ID);

        System.out.println("Got contract " + contract.id);

        /* Information button */
        ImageButton imageButton = (ImageButton) findViewById(R.id.imageButtonContract);
        if (imageButton != null) {
            imageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                     /* Start the info activity */
                    Intent intent = new Intent(getBaseContext(), InfoActivity.class);
                    startActivity(intent);
                }
            });
        }


        /* Send request to server to get all the contract details */
        CommManager.requestContract(this,contract.id);
    }


    /* Show contract details in the contract view */
    private void displayContract() {

        /* Show contract title */
        TextView tv = (TextView)findViewById(R.id.textViewTitluContract);
        if (tv != null)
            tv.setText(contract.title);

        /* Show Contract number */
        tv = (TextView)findViewById(R.id.textContractNr);
        if (tv != null)
            tv.setText(contract.number);

        /* Show company */
        tv = (TextView)findViewById(R.id.textCompany);
        if (tv != null) {
            tv.setText(contract.company.name);
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    System.out.println("Click on company");

                    /* Start a separate view for a company */
                    Intent intent = new Intent(getBaseContext(), ContractListActivity.class);
                    intent.putExtra(ContractListActivity.CONTRACT_LIST_TYPE, ContractListActivity.CONTRACT_LIST_FOR_COMPANY);
                    intent.putExtra(ContractListActivity.CONTRACT_LIST_EXTRA, contract.company.name);
                    startActivity(intent);
                }
            });
        }

        /* Show the buyeer */
        tv = (TextView)findViewById(R.id.textPrimarie);
        if (tv != null) {
            tv.setText(contract.authority);
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    System.out.println("Click on authority");

                    /* Start a separate view for a company */
                    Intent intent = new Intent(getBaseContext(), ContractListActivity.class);
                    intent.putExtra(ContractListActivity.CONTRACT_LIST_TYPE, ContractListActivity.CONTRACT_LIST_FOR_BUYER);
                    intent.putExtra(ContractListActivity.CONTRACT_LIST_EXTRA, contract.authority);
                    startActivity(intent);
                }
            });
        }

        /* Show the date of the contract */
        tv = (TextView)findViewById(R.id.textData);
        if (tv != null)
            tv.setText(contract.date);

        /* Show the value of the contract */
        tv = (TextView)findViewById(R.id.textValue);
        double price = Double.parseDouble(contract.valueEUR);
        DecimalFormat dm = new DecimalFormat("###,###.###");
        if (tv != null)
            tv.setText(String.valueOf(dm.format(price)) + " EUR");

        /* Show the contract's CPV code */
        tv = (TextView)findViewById(R.id.textCPV);
        if (tv != null)
            tv.setText(contract.CPVCode);


        /* Setup the Justify button */
        System.out.println("Contract votes " + contract.votes);
        Button button = (Button)findViewById(R.id.button);
        if (button != null) {
            button.setText("Cere justificare (" + contract.votes + ")");
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    /* Check whether you have voted before */
                    int votedBefore = just_prefs.getInt("Contract" + contract.id, -1);
                    System.out.println("Saved prefference value for contract " + contract.id + " id " + votedBefore);

                    if (votedBefore == -1) {
                        System.out.println("Never voted. Calling justify from button");
                        CommManager.justifyContract(contractContext, contract);
                    } else
                        Toast.makeText(contractContext, "Ai mai cerut o data justificarea acestui contract",
                                Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /* Update the Justify button when the justify succeeds */
    public void ackJustify() {
        contract.votes++;
        Button button = (Button)findViewById(R.id.button);
        if (button != null) {
            button.setText("Justifica (" + contract.votes + ")");
        }

        /* Remember the fact that you voted for this contract */
        SharedPreferences.Editor editor = just_prefs.edit();
        editor.putInt("Contract" + contract.id, 1);
        editor.commit();

        Toast.makeText(this, "Cererea ta a fost inregistrata. La " + MINIMUM_VOTES +
                " de cereri Initiativa Romania va cere detalii despre acest contract si anexele sale.", Toast.LENGTH_LONG).show();
    }


    /* Receive the contract details from the server and display them */
    public void receiveContract(JSONObject response) {

        try {
            contract.CPVCode = response.getString("CPVCode");
            contract.address = response.getString("address");
            contract.company = new Company();
            contract.authority = response.getString("buyer");
            contract.votes = Integer.parseInt(response.getString("justify"));
            contract.company.name = response.getString("company");
            contract.number = response.getString("contract_nr");
            contract.title = response.getString("contract_title");
            contract.valueEUR = response.getString("price");
            contract.date = response.getString("start_date");

            /* Save all the categories */
            JSONArray categoriesJSON = response.getJSONArray("categories");
            for (int i = 0; i < categoriesJSON.length(); i++) {

                Category category = new Category();
                category.name = categoriesJSON.get(i).toString();
                contract.addCategory(category);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        displayContract();
    }
}