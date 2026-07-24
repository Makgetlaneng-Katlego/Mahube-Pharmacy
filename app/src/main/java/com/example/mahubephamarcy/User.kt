package com.example.mahubephamarcy

class User {
    var fullname:String?=null;
    var email:String?=null;
    var phoneNumber:String?=null;
    var password: String?=null;

    fun storeNames(fullname: String,email:String,phoneNumber:String,password: String){
        this.fullname=fullname;
        this.email=email;
        this.phoneNumber=phoneNumber;
        this.password=password

    }




}