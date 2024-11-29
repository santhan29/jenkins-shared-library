def getAccountID(String environment){
    switch(environment) {
        case 'dev':
            return "361769595563"
        case 'qa':
            return "361769595563"
        case 'pre-prod':
            return "361769595563" 
        case 'uat':
            return "361769595563" 
        case 'prod':
            return "361769595563" 
        default:
            return "nothing" 
    }
}