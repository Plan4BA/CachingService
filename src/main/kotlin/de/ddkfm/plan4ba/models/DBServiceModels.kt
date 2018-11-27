package de.ddkfm.plan4ba.models

data class User(
        var id: Int,
        var matriculationNumber: String,
        var userHash : String?,
        var password : String,
        var groupId : Int,
        var lastLecturePolling : Long,
        var lastLectureCall : Long
)

data class Token(
        var token : String,
        var userId : Int,
        var isCalDavToken : Boolean,
        var isRefreshToken : Boolean,
        var validTo : Long
)

data class UserGroup(
        var id : Int,
        var uid : String,
        var universityId : Int
)

data class University(
        var id : Int,
        var name : String,
        var accentColor : String,
        var logoUrl : String
)

data class Meal(
        var universityId: Int,
        var day : Long,
        var meals : List<Food>

)

data class Food(
        var description: String,
        var prices : String,
        var vegetarian : Boolean,
        var vegan : Boolean,
        var additionalInformation : String
)

data class Lecture(
        var id : Int,
        var title : String,
        var start : Long,
        var end : Long,
        var allDay : Boolean,
        var description : String,
        var color : String,
        var room : String,
        var sroom : String,
        var instructor : String,
        var remarks : String,
        var exam : Boolean,
        var userId : Int
)

