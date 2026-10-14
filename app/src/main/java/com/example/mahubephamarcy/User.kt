package com.example.mahubephamarcy

class User {
    private var fullname:String?=null;
    private var email:String?=null;
    private var phoneNumber:String?=null;
    private var password: String?=null;

    // Getters for the users class
    fun getFullname():String?{
        return this.fullname;
    }
    fun getEmail():String?{
        return this.email
    }
    fun getPhoneNumber():String?{
        return this.phoneNumber
    }
    fun getPassword():String?{
        return this.password
    }
    //setters for the user class
    fun setEmail(Email:String){
        this.email=Email;
    }
    fun setFullname(Fullname:String){
        this.fullname=Fullname;
    }
    fun setPhoneNumber(PhoneNumber:String){
        this.phoneNumber=PhoneNumber
    }
    fun setPassword(Password:String){
        this.password=Password
    }
}